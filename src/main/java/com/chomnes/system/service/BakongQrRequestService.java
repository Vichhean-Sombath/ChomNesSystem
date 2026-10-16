package com.chomnes.system.service;

import com.chomnes.system.dto.BakongQrRequestResponse;
import com.chomnes.system.dto.CreateBakongQrRequest;
import com.chomnes.system.exception.BakongQrRequestConflictException;
import com.chomnes.system.exception.BakongQrRequestNotFoundException;
import com.chomnes.system.exception.InvoiceNotFoundException;
import com.chomnes.system.model.BakongQrRequest;
import com.chomnes.system.model.Invoice;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.BakongQrRequestRepository;
import com.chomnes.system.repository.InvoiceRepository;
import com.chomnes.system.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class BakongQrRequestService {
    private final BakongQrRequestRepository requestRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;

    public BakongQrRequestService(
        BakongQrRequestRepository requestRepository,
        InvoiceRepository invoiceRepository,
        StudentRepository studentRepository
    ) {
        this.requestRepository = requestRepository;
        this.invoiceRepository = invoiceRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public List<BakongQrRequestResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return requestRepository.findAllByOrderByCreatedAtDescQrIdDesc().stream()
                .map(this::expireIfNeeded)
                .map(BakongQrRequestResponse::from)
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
            return requestRepository.findAllByInvoiceIdInOrderByCreatedAtDescQrIdDesc(invoiceIds).stream()
                .map(this::expireIfNeeded)
                .map(BakongQrRequestResponse::from)
                .toList();
        }
        throw new AccessDeniedException("You cannot access Bakong QR requests.");
    }

    @Transactional
    public BakongQrRequestResponse findById(Integer qrId, Integer requesterId, User.Role requesterRole) {
        BakongQrRequest request = requestRepository.findById(qrId)
            .orElseThrow(() -> new BakongQrRequestNotFoundException(qrId));
        authorizeInvoiceOwner(request.getInvoiceId(), requesterId, requesterRole);
        expireIfNeeded(request);
        return BakongQrRequestResponse.from(request);
    }

    public BakongQrRequestResponse create(
        CreateBakongQrRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        if (requesterRole != User.Role.Admin && requesterRole != User.Role.Student) {
            throw new AccessDeniedException("Only an administrator or the invoice owner can create a Bakong QR request.");
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(request.invoiceId())
            .orElseThrow(() -> new InvoiceNotFoundException(request.invoiceId()));
        authorizeInvoiceOwner(invoice, requesterId, requesterRole);

        BigDecimal remainingBalance = invoice.getTotalAmount().subtract(invoice.getAmountPaid());
        if (remainingBalance.signum() <= 0) {
            throw new BakongQrRequestConflictException("This invoice is already fully paid.");
        }
        if (request.amountUsd().compareTo(remainingBalance) > 0) {
            throw new BakongQrRequestConflictException("QR amount cannot exceed the invoice's remaining balance.");
        }
        LocalDateTime now = LocalDateTime.now();
        requestRepository.findAllByInvoiceIdAndStatus(invoice.getInvoiceId(), "Pending").stream()
            .filter(existing -> !existing.getExpiresAt().isAfter(now))
            .forEach(existing -> existing.setStatus("Expired"));
        if (requestRepository.existsByInvoiceIdAndStatus(invoice.getInvoiceId(), "Pending")) {
            throw new BakongQrRequestConflictException("A pending Bakong QR request already exists for this invoice.");
        }
        if (requestRepository.findByMd5(request.md5()).isPresent()) {
            throw new BakongQrRequestConflictException("This Bakong transaction hash is already associated with a request.");
        }

        BakongQrRequest qrRequest = new BakongQrRequest(
            invoice.getInvoiceId(),
            request.md5(),
            request.amountKhr(),
            request.amountUsd(),
            request.expiresAt()
        );
        return BakongQrRequestResponse.from(requestRepository.save(qrRequest));
    }

    @Transactional
    public BakongQrRequestResponse createGenerated(
        Integer invoiceId,
        String md5,
        BigDecimal amountKhr,
        BigDecimal amountUsd,
        LocalDateTime expiresAt,
        Integer requesterId,
        User.Role requesterRole
    ) {
        if (requesterRole != User.Role.Admin && requesterRole != User.Role.Student) {
            throw new AccessDeniedException("Only an administrator or the invoice owner can create a Bakong QR request.");
        }
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        authorizeInvoiceOwner(invoice, requesterId, requesterRole);
        BigDecimal remainingBalance = invoice.getTotalAmount().subtract(invoice.getAmountPaid());
        if (remainingBalance.signum() <= 0 || amountUsd.signum() <= 0 || amountUsd.compareTo(remainingBalance) > 0) {
            throw new BakongQrRequestConflictException("QR amount must be positive and cannot exceed the invoice's remaining balance.");
        }
        if (requestRepository.findByMd5(md5).isPresent()) {
            throw new BakongQrRequestConflictException("This Bakong transaction hash is already associated with a request.");
        }
        LocalDateTime now = LocalDateTime.now();
        requestRepository.findAllByInvoiceIdAndStatus(invoiceId, "Pending").stream()
            .filter(existing -> !existing.getExpiresAt().isAfter(now))
            .forEach(existing -> existing.setStatus("Expired"));
        if (requestRepository.existsByInvoiceIdAndStatus(invoiceId, "Pending")) {
            throw new BakongQrRequestConflictException("A pending Bakong QR request already exists for this invoice.");
        }
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("Bakong QR expiration must be in the future.");
        }
        BakongQrRequest saved = requestRepository.save(new BakongQrRequest(
            invoiceId, md5, amountKhr.setScale(2, RoundingMode.HALF_UP),
            amountUsd.setScale(2, RoundingMode.HALF_UP), expiresAt));
        return BakongQrRequestResponse.from(saved);
    }

    @Transactional
    public BakongQrRequest findByMd5ForRequester(String md5, Integer requesterId, User.Role requesterRole) {
        BakongQrRequest request = requestRepository.findByMd5ForUpdate(md5)
            .orElseThrow(() -> new BakongQrRequestNotFoundException(md5));
        authorizeInvoiceOwner(request.getInvoiceId(), requesterId, requesterRole);
        expireIfNeeded(request);
        return request;
    }

    @Transactional(readOnly = true)
    public BakongQrRequest findByMd5(String md5) {
        return requestRepository.findByMd5(md5).orElse(null);
    }

    @Transactional
    public BakongQrRequest markStatus(String md5, String status) {
        BakongQrRequest request = requestRepository.findByMd5ForUpdate(md5)
            .orElseThrow(() -> new BakongQrRequestNotFoundException(md5));
        if ("Pending".equals(request.getStatus())) {
            request.setStatus(status);
            if ("Paid".equals(status)) request.setPaidAt(LocalDateTime.now());
        }
        return request;
    }

    private void authorizeInvoiceOwner(Integer invoiceId, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole != User.Role.Student) {
            throw new AccessDeniedException("You cannot access this Bakong QR request.");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        authorizeInvoiceOwner(invoice, requesterId, requesterRole);
    }

    private void authorizeInvoiceOwner(Invoice invoice, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        Student student = getStudentForUser(requesterId);
        if (!student.getStudentId().equals(invoice.getStudentId())) {
            throw new AccessDeniedException("Students can only access Bakong QR requests for their own invoices.");
        }
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
            .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private BakongQrRequest expireIfNeeded(BakongQrRequest request) {
        if ("Pending".equals(request.getStatus()) && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            request.setStatus("Expired");
        }
        return request;
    }
}
