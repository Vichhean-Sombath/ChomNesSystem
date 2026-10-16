package com.chomnes.system.service;

import com.chomnes.system.dto.CreatePaymentRequest;
import com.chomnes.system.dto.PaymentResponse;
import com.chomnes.system.dto.UpdatePaymentRequest;
import com.chomnes.system.exception.InvoiceNotFoundException;
import com.chomnes.system.exception.PaymentConflictException;
import com.chomnes.system.exception.PaymentNotFoundException;
import com.chomnes.system.exception.UserNotFoundException;
import com.chomnes.system.model.Invoice;
import com.chomnes.system.model.Payment;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.InvoiceRepository;
import com.chomnes.system.repository.PaymentRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            StudentRepository studentRepository,
            UserRepository userRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return paymentRepository.findAllByOrderByPaymentDateDescPaymentIdDesc().stream()
                    .map(PaymentResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            List<Integer> invoiceIds = invoiceRepository
                    .findAllByStudentIdOrderByCreatedAtDescInvoiceIdDesc(student.getStudentId()).stream()
                    .map(Invoice::getInvoiceId)
                    .toList();
            if (invoiceIds.isEmpty()) {
                return List.of();
            }
            return paymentRepository.findAllByInvoiceIdInOrderByPaymentDateDescPaymentIdDesc(invoiceIds).stream()
                    .map(PaymentResponse::from)
                    .toList();
        }
        throw new AccessDeniedException("You cannot access payments.");
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(Integer paymentId, Integer requesterId, User.Role requesterRole) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        authorizeInvoiceOwner(payment.getInvoiceId(), requesterId, requesterRole);
        return PaymentResponse.from(payment);
    }

    public PaymentResponse recordPayment(
            CreatePaymentRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        if (requesterRole != User.Role.Admin) {
            throw new AccessDeniedException("Only an administrator can record a payment.");
        }
        if (request.paymentDate().isAfter(LocalDate.now())) {
            throw new PaymentConflictException("Payment date cannot be in the future.");
        }
        String paymentMethod = normalizePaymentMethod(request.paymentMethod());
        Integer recordedBy = request.recordedBy() == null ? requesterId : request.recordedBy();
        if (!userRepository.existsById(recordedBy)) throw new UserNotFoundException(recordedBy);
        if (request.transactionReference() != null
                && paymentRepository.existsByTransactionReference(request.transactionReference())) {
            throw new PaymentConflictException("This transaction reference has already been recorded.");
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(request.invoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(request.invoiceId()));
        BigDecimal remainingBalance = invoice.getTotalAmount().subtract(invoice.getAmountPaid());
        if (remainingBalance.signum() <= 0) {
            throw new PaymentConflictException("This invoice is already fully paid.");
        }
        if (request.amount().compareTo(remainingBalance) > 0) {
            throw new PaymentConflictException("Payment amount cannot exceed the invoice's remaining balance.");
        }

        Payment payment = new Payment(
                invoice.getInvoiceId(),
                request.paymentDate(),
                request.amount(),
                paymentMethod,
                request.receiptUrl(),
                request.transactionReference(),
                recordedBy,
                request.notes()
        );
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(request.amount());
        invoice.setAmountPaid(newAmountPaid);
        if (newAmountPaid.compareTo(invoice.getTotalAmount()) == 0) {
            invoice.setStatus("Paid");
        } else {
            invoice.setStatus("Partial");
        }
        invoiceRepository.save(invoice);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public PaymentResponse update(Integer paymentId, UpdatePaymentRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one payment field to update.");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Integer oldInvoiceId = payment.getInvoiceId();
        Integer invoiceId = request.invoiceId() == null ? oldInvoiceId : request.invoiceId();
        if (!invoiceRepository.existsById(invoiceId)) throw new InvoiceNotFoundException(invoiceId);
        if (request.recordedBy() != null && !userRepository.existsById(request.recordedBy())) {
            throw new UserNotFoundException(request.recordedBy());
        }
        if (request.transactionReference() != null && !request.transactionReference().equals(payment.getTransactionReference())
                && paymentRepository.existsByTransactionReferenceAndPaymentIdNot(request.transactionReference(), paymentId)) {
            throw new PaymentConflictException("This transaction reference has already been recorded.");
        }
        if (request.paymentDate() != null) payment.setPaymentDate(request.paymentDate());
        if (request.amount() != null) payment.setAmount(request.amount());
        if (request.paymentMethod() != null) payment.setPaymentMethod(normalizePaymentMethod(request.paymentMethod()));
        if (request.receiptUrl() != null) payment.setReceiptUrl(request.receiptUrl());
        if (request.transactionReference() != null) payment.setTransactionReference(request.transactionReference());
        if (request.recordedBy() != null) payment.setRecordedBy(request.recordedBy());
        if (request.notes() != null) payment.setNotes(request.notes());
        payment.setInvoiceId(invoiceId);
        Payment saved = paymentRepository.save(payment);
        recalculateInvoice(oldInvoiceId);
        if (!oldInvoiceId.equals(invoiceId)) recalculateInvoice(invoiceId);
        return PaymentResponse.from(saved);
    }

    public void delete(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Integer invoiceId = payment.getInvoiceId();
        paymentRepository.delete(payment);
        recalculateInvoice(invoiceId);
    }

    private void recalculateInvoice(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        BigDecimal totalPaid = paymentRepository.findAllByInvoiceIdOrderByPaymentDateDescPaymentIdDesc(invoiceId)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setAmountPaid(totalPaid);
        invoice.setStatus(totalPaid.compareTo(invoice.getTotalAmount()) >= 0
                ? "Paid" : totalPaid.signum() > 0 ? "Partial" : "Unpaid");
        invoiceRepository.save(invoice);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if ("stripe".equalsIgnoreCase(paymentMethod)) return "Stripe";
        if ("bakongkhqr".equalsIgnoreCase(paymentMethod)) return "BakongKHQR";
        throw new IllegalArgumentException("Payment method must be Stripe or BakongKHQR.");
    }

    private void authorizeInvoiceOwner(Integer invoiceId, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole != User.Role.Student) {
            throw new AccessDeniedException("You cannot access payments.");
        }
        Student student = getStudentForUser(requesterId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        if (!student.getStudentId().equals(invoice.getStudentId())) {
            throw new AccessDeniedException("Students can only access payments for their own invoices.");
        }
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }
}
