package com.chomnes.system.service;

import com.chomnes.system.dto.BakongQrRequestResponse;
import com.chomnes.system.dto.PaymentResponse;
import com.chomnes.system.exception.InvoiceNotFoundException;
import com.chomnes.system.exception.PaymentConflictException;
import com.chomnes.system.model.BakongQrRequest;
import com.chomnes.system.model.FeeStructure;
import com.chomnes.system.model.Invoice;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.FeeStructureRepository;
import com.chomnes.system.repository.InvoiceRepository;
import com.chomnes.system.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class PaymentIntegrationService {
    private final StripeProviderClient stripe;
    private final BakongProviderClient bakong;
    private final PaymentService payments;
    private final BakongQrRequestService qrRequests;
    private final InvoiceRepository invoices;
    private final FeeStructureRepository fees;
    private final StudentRepository students;

    public PaymentIntegrationService(
            StripeProviderClient stripe,
            BakongProviderClient bakong,
            PaymentService payments,
            BakongQrRequestService qrRequests,
            InvoiceRepository invoices,
            FeeStructureRepository fees,
            StudentRepository students
    ) {
        this.stripe = stripe;
        this.bakong = bakong;
        this.payments = payments;
        this.qrRequests = qrRequests;
        this.invoices = invoices;
        this.fees = fees;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stripeConfig() {
        return Map.of("public_key", stripe.publicKey());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> createStripeCheckout(
            Integer invoiceId,
            String successUrl,
            String cancelUrl,
            Integer requesterId,
            User.Role requesterRole
    ) {
        Invoice invoice = getInvoice(invoiceId);
        authorizeInvoiceOwner(invoice, requesterId, requesterRole);
        BigDecimal remaining = remainingBalance(invoice);
        if (remaining.signum() <= 0) throw new PaymentConflictException("This invoice is already fully paid.");
        long amountCents = remaining.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        FeeStructure fee = fees.findById(invoice.getFeeId()).orElse(null);
        String feeName = fee == null ? "School fee" : fee.getFeeName();
        StripeProviderClient.StripeSession session = stripe.createCheckoutSession(invoice.getInvoiceId(),
                invoice.getStudentId(), invoice.getInvoiceNumber(), feeName, amountCents, successUrl, cancelUrl);
        if (session.id() == null || session.url() == null) {
            throw new IllegalStateException("Stripe did not return an active checkout session URL.");
        }
        return Map.of("session_id", session.id(), "url", session.url());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stripeSuccess(String sessionId) {
        StripeProviderClient.StripeSession session = stripe.retrieveCheckoutSession(sessionId);
        return object("session_id", session.id(), "status", session.paymentStatus(), "invoice_id", session.invoiceId());
    }

    public PaymentResponse verifyStripeCheckout(String sessionId, Integer requesterId, User.Role requesterRole) {
        StripeProviderClient.StripeSession session = stripe.retrieveCheckoutSession(sessionId);
        if (session.id() == null || !"paid".equalsIgnoreCase(session.paymentStatus()) || session.amountTotal() == null
                || !"usd".equalsIgnoreCase(session.currency()) || session.invoiceId() == null) {
            throw new IllegalArgumentException("Payment not completed or invoice not found.");
        }
        Integer invoiceId = parseId(session.invoiceId(), "Stripe session invoice ID is invalid.");
        Invoice invoice = getInvoice(invoiceId);
        authorizeInvoiceOwner(invoice, requesterId, requesterRole);
        if (session.studentId() != null && !session.studentId().equals(invoice.getStudentId().toString())) {
            throw new IllegalArgumentException("Stripe session does not match the invoice owner.");
        }
        BigDecimal amount = BigDecimal.valueOf(session.amountTotal(), 2);
        return payments.recordGatewayPayment(invoiceId, amount, "Stripe", session.id(), null);
    }

    public void handleStripeWebhook(byte[] rawBody, String signature) {
        JsonNode event = stripe.verifyAndParseWebhook(rawBody, signature);
        String type = event.path("type").asText("");
        if (!"checkout.session.completed".equals(type) && !"checkout.session.async_payment_succeeded".equals(type)) return;
        String sessionId = event.path("data").path("object").path("id").asText(null);
        if (sessionId != null) verifyStripeCheckout(sessionId, null, User.Role.Admin);
    }

    public Map<String, Object> generateBakongQr(Integer invoiceId, Integer requesterId, User.Role requesterRole) {
        Invoice invoice = getInvoice(invoiceId);
        authorizeInvoiceOwner(invoice, requesterId, requesterRole);
        BigDecimal remaining = remainingBalance(invoice);
        if (remaining.signum() <= 0) throw new PaymentConflictException("This invoice is already fully paid.");
        BakongProviderClient.GeneratedKhqr generated = bakong.generate(remaining, invoice.getInvoiceNumber());
        if (generated.md5() == null || !generated.md5().matches("(?i)[a-f0-9]{32}")) {
            throw new IllegalStateException("KHQR generation did not return a valid MD5 transaction key.");
        }
        LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(generated.expirationTimestamp()),
                ZoneId.systemDefault());
        BakongQrRequestResponse saved = qrRequests.createGenerated(invoice.getInvoiceId(), generated.md5(),
                generated.amountKhr(), generated.amountUsd(), expiresAt, requesterId, requesterRole);
        return object("qr_string", generated.qrString(), "md5", generated.md5(), "qr_id", saved.qrId(),
                "amount", generated.amountKhr(), "currency", "KHR", "invoice_id", invoice.getInvoiceId(),
                "invoice_number", invoice.getInvoiceNumber(), "expiration_timestamp", generated.expirationTimestamp(),
                "verified", generated.verified());
    }

    public Map<String, Object> verifyBakong(String md5, Integer requesterId, User.Role requesterRole) {
        if (md5 == null || !md5.matches("(?i)[a-f0-9]{32}")) {
            throw new IllegalArgumentException("md5 must be a 32-character hexadecimal value.");
        }
        BakongQrRequest qr = qrRequests.findByMd5ForRequester(md5, requesterId, requesterRole);
        return pollAndApplyBakong(qr, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkBakongAccount() {
        return bakong.checkAccount();
    }

    public Map<String, Object> handleBakongWebhook(JsonNode payload) {
        String md5 = firstText(payload.path("md5"), payload.path("data").path("md5"));
        if (md5 == null || !md5.matches("(?i)[a-f0-9]{32}")) return Map.of("received", true);
        if (qrRequests.findByMd5(md5) == null) return Map.of("received", true);
        BakongQrRequest request = qrRequests.findByMd5ForRequester(md5, null, User.Role.Admin);
        if ("Pending".equals(request.getStatus())) pollAndApplyBakong(request, false);
        return Map.of("received", true);
    }

    private Map<String, Object> pollAndApplyBakong(BakongQrRequest qr, boolean includeStatus) {
        String md5 = qr.getMd5();
        JsonNode result = null;
        Integer paymentId = null;
        if ("Pending".equals(qr.getStatus())) {
            result = bakong.checkTransactionByMd5(md5);
            JsonNode transaction = result.path("data");
            if (result.path("responseCode").asInt(-1) == 0 && transaction.isObject()) {
                String currency = transaction.path("currency").asText("KHR").toUpperCase();
                BigDecimal rawAmount = decimal(transaction.path("amount"));
                String receivedAccount = transaction.path("toAccountId").asText("");
                if (receivedAccount.isBlank() || !receivedAccount.equalsIgnoreCase(bakong.accountId())) {
                    throw new IllegalArgumentException("Bakong transaction was paid to a different merchant account.");
                }
                BigDecimal amountUsd;
                boolean matched;
                if ("USD".equals(currency)) {
                    amountUsd = rawAmount;
                    matched = rawAmount.subtract(qr.getAmountUsd()).abs().compareTo(new BigDecimal("0.01")) <= 0;
                } else if ("KHR".equals(currency)) {
                    amountUsd = rawAmount.divide(bakong.khrPerUsd(), 2, RoundingMode.HALF_UP);
                    matched = rawAmount.subtract(qr.getAmountKhr()).abs().compareTo(BigDecimal.ONE) <= 0;
                } else {
                    amountUsd = BigDecimal.ZERO;
                    matched = false;
                }
                if (rawAmount.signum() > 0 && matched) {
                    String transactionHash = transaction.path("hash").asText(md5);
                    PaymentResponse saved = payments.recordGatewayPayment(qr.getInvoiceId(), amountUsd,
                            "BakongKHQR", transactionHash, transaction.path("receiptUrl").asText(null));
                    paymentId = saved.paymentId();
                    qrRequests.markStatus(md5, "Paid");
                } else if (rawAmount.signum() > 0) {
                    qrRequests.markStatus(md5, "Failed");
                }
            }
        }
        BakongQrRequest refreshed = qrRequests.findByMd5ForRequester(md5, null, User.Role.Admin);
        Map<String, Object> response = object("md5", refreshed.getMd5(), "invoice_id", refreshed.getInvoiceId(),
                "status", refreshed.getStatus(), "paid", "Paid".equals(refreshed.getStatus()),
                "expired", "Expired".equals(refreshed.getStatus()), "amount_khr", refreshed.getAmountKhr(),
                "amount_usd", refreshed.getAmountUsd(), "expires_at", refreshed.getExpiresAt(),
                "paid_at", refreshed.getPaidAt(), "payment_id", paymentId);
        if (includeStatus) response.put("bakong_status", result);
        return response;
    }

    private Invoice getInvoice(Integer invoiceId) {
        return invoices.findById(invoiceId).orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    private void authorizeInvoiceOwner(Invoice invoice, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) return;
        if (requesterRole != User.Role.Student) throw new AccessDeniedException("You are not authorized to pay this invoice.");
        Student student = students.findByUserId(requesterId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
        if (!student.getStudentId().equals(invoice.getStudentId())) {
            throw new AccessDeniedException("You are not authorized to pay this invoice.");
        }
    }

    private BigDecimal remainingBalance(Invoice invoice) {
        return invoice.getTotalAmount().subtract(invoice.getAmountPaid()).max(BigDecimal.ZERO);
    }

    private Integer parseId(String value, String message) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal decimal(JsonNode value) {
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private String firstText(JsonNode first, JsonNode second) {
        String value = first.isTextual() ? first.asText() : second.isTextual() ? second.asText() : null;
        return value == null || value.isBlank() ? null : value;
    }

    private Map<String, Object> object(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
}
