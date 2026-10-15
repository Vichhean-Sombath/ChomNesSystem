package com.chomnes.system.service;

import com.chomnes.system.dto.CreatePaymentRequest;
import com.chomnes.system.dto.PaymentResponse;
import com.chomnes.system.exception.InvoiceNotFoundException;
import com.chomnes.system.exception.PaymentConflictException;
import com.chomnes.system.exception.PaymentNotFoundException;
import com.chomnes.system.model.Invoice;
import com.chomnes.system.model.Payment;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.InvoiceRepository;
import com.chomnes.system.repository.PaymentRepository;
import com.chomnes.system.repository.StudentRepository;
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

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            StudentRepository studentRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.studentRepository = studentRepository;
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
        if (request.transactionReference() != null
                && paymentRepository.existsByPaymentMethodAndTransactionReference(
                        request.paymentMethod(), request.transactionReference()
                )) {
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
                request.paymentMethod(),
                request.receiptUrl(),
                request.transactionReference(),
                requesterId,
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
