package com.chomnes.system.service;

import com.chomnes.system.model.FeeStructure;
import com.chomnes.system.model.Invoice;
import com.chomnes.system.model.Payment;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.StudentEmergencyContact;
import com.chomnes.system.repository.FeeStructureRepository;
import com.chomnes.system.repository.InvoiceRepository;
import com.chomnes.system.repository.PaymentRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.StudentEmergencyContactRepository;
import com.chomnes.system.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Properties;
import java.time.Duration;

@Service
public class PaymentNotificationService {
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final StudentRepository students;
    private final FeeStructureRepository fees;
    private final SemesterRepository semesters;
    private final StudentEmergencyContactRepository contacts;
    private final ObjectMapper json;
    private final String telegramToken;
    private final String telegramChatId;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String mailFrom;

    public PaymentNotificationService(
            PaymentRepository payments,
            InvoiceRepository invoices,
            StudentRepository students,
            FeeStructureRepository fees,
            SemesterRepository semesters,
            StudentEmergencyContactRepository contacts,
            ObjectMapper json,
            @Value("${app.notifications.telegram-token:}") String telegramToken,
            @Value("${app.notifications.telegram-chat-id:}") String telegramChatId,
            @Value("${app.notifications.smtp-host:}") String smtpHost,
            @Value("${app.notifications.smtp-port:587}") int smtpPort,
            @Value("${app.notifications.smtp-username:}") String smtpUsername,
            @Value("${app.notifications.smtp-password:}") String smtpPassword,
            @Value("${app.notifications.mail-from:}") String mailFrom
    ) {
        this.payments = payments;
        this.invoices = invoices;
        this.students = students;
        this.fees = fees;
        this.semesters = semesters;
        this.contacts = contacts;
        this.json = json;
        this.telegramToken = telegramToken;
        this.telegramChatId = telegramChatId;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.mailFrom = mailFrom;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void notifyRecordedPayment(Integer paymentId) {
        try {
            Payment payment = payments.findById(paymentId).orElse(null);
            if (payment == null) return;
            Invoice invoice = invoices.findById(payment.getInvoiceId()).orElse(null);
            if (invoice == null) return;
            Student student = students.findById(invoice.getStudentId()).orElse(null);
            FeeStructure fee = fees.findById(invoice.getFeeId()).orElse(null);
            Semester semester = semesters.findById(invoice.getSemesterId()).orElse(null);
            StudentEmergencyContact contact = contacts.findFirstByStudentIdAndEmailIsNotNullOrderByContactIdAsc(
                    invoice.getStudentId()).orElse(null);
            String studentName = student == null ? "Unknown student"
                    : student.getFirstName() + " " + student.getLastName();
            Context context = new Context(studentName, invoice.getInvoiceNumber(),
                    fee == null ? "Unknown fee" : fee.getFeeName(),
                    semester == null ? "" : semester.getSemesterName(), payment.getAmount(),
                    payment.getPaymentMethod(), payment.getPaymentDate(), payment.getTransactionReference(),
                    payment.getReceiptUrl(), contact == null ? null : contact.getContactName(),
                    contact == null ? null : contact.getEmail());
            sendTelegram(context);
            sendReceiptEmail(context);
        } catch (Exception ignored) {
            // Optional notifications must never roll back or fail a confirmed payment.
        }
    }

    private void sendTelegram(Context context) {
        if (blank(telegramToken) || blank(telegramChatId)) return;
        try {
            String message = "<b>New Payment Received</b>\n\n"
                    + "<b>Student:</b> " + html(context.student()) + "\n"
                    + "<b>Invoice:</b> " + html(context.invoiceNumber()) + "\n"
                    + "<b>Fee:</b> " + html(context.feeName()) + "\n"
                    + (blank(context.semesterName()) ? "" : "<b>Semester:</b> " + html(context.semesterName()) + "\n")
                    + "<b>Amount:</b> $" + money(context.amount()) + "\n"
                    + "<b>Method:</b> " + html(context.paymentMethod()) + "\n"
                    + "<b>Date:</b> " + html(context.paymentDate()) + "\n"
                    + (blank(context.transactionReference()) ? "" : "<b>Reference:</b> " + html(context.transactionReference()) + "\n")
                    + (blank(context.receiptUrl()) ? "" : "<b>Receipt:</b> " + html(context.receiptUrl()));
            String body = json.writeValueAsString(java.util.Map.of(
                    "chat_id", telegramChatId, "text", message, "parse_mode", "HTML"));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + telegramToken + "/sendMessage"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) { }
    }

    private void sendReceiptEmail(Context context) {
        if (blank(smtpHost) || blank(smtpUsername) || blank(smtpPassword)
                || blank(mailFrom) || blank(context.contactEmail())) return;
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(smtpHost);
            sender.setPort(smtpPort);
            sender.setUsername(smtpUsername);
            sender.setPassword(smtpPassword);
            sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
            Properties properties = sender.getJavaMailProperties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.connectiontimeout", "5000");
            properties.put("mail.smtp.timeout", "5000");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(context.contactEmail());
            message.setSubject("Payment Receipt - " + context.invoiceNumber());
            message.setText("Dear " + safe(context.contactName()) + ",\n\n"
                    + "A payment has been received for " + context.student() + ".\n"
                    + "Invoice: " + context.invoiceNumber() + "\n"
                    + "Fee: " + context.feeName() + "\n"
                    + (blank(context.semesterName()) ? "" : "Semester: " + context.semesterName() + "\n")
                    + "Amount: $" + money(context.amount()) + "\n"
                    + "Payment method: " + context.paymentMethod() + "\n"
                    + "Payment date: " + context.paymentDate() + "\n"
                    + (blank(context.transactionReference()) ? "" : "Reference: " + context.transactionReference() + "\n")
                    + (blank(context.receiptUrl()) ? "" : "Receipt: " + context.receiptUrl() + "\n")
                    + "\nThank you,\nChomNes System");
            sender.send(message);
        } catch (Exception ignored) { }
    }

    private String money(BigDecimal amount) { return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private String html(Object value) { return safe(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String safe(Object value) { return value == null ? "" : value.toString(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private record Context(String student, String invoiceNumber, String feeName, String semesterName,
                           BigDecimal amount, String paymentMethod, LocalDate paymentDate,
                           String transactionReference, String receiptUrl, String contactName,
                           String contactEmail) { }
}
