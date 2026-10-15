package com.chomnes.system.service;

import com.chomnes.system.dto.CreateInvoiceRequest;
import com.chomnes.system.dto.InvoiceResponse;
import com.chomnes.system.exception.FeeStructureNotFoundException;
import com.chomnes.system.exception.InvoiceConflictException;
import com.chomnes.system.exception.InvoiceNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.model.ClassEnrollment;
import com.chomnes.system.model.FeeStructure;
import com.chomnes.system.model.Invoice;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.FeeStructureRepository;
import com.chomnes.system.repository.InvoiceRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final SchoolClassRepository schoolClassRepository;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            FeeStructureRepository feeStructureRepository,
            StudentRepository studentRepository,
            ClassEnrollmentRepository enrollmentRepository,
            SchoolClassRepository schoolClassRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return invoiceRepository.findAllByOrderByCreatedAtDescInvoiceIdDesc().stream()
                    .map(InvoiceResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            return invoiceRepository.findAllByStudentIdOrderByCreatedAtDescInvoiceIdDesc(student.getStudentId())
                    .stream()
                    .map(InvoiceResponse::from)
                    .toList();
        }
        throw new AccessDeniedException("You cannot access invoices.");
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Integer invoiceId, Integer requesterId, User.Role requesterRole) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        if (requesterRole == User.Role.Admin) {
            return InvoiceResponse.from(invoice);
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!student.getStudentId().equals(invoice.getStudentId())) {
                throw new AccessDeniedException("Students can only access their own invoices.");
            }
            return InvoiceResponse.from(invoice);
        }
        throw new AccessDeniedException("You cannot access invoices.");
    }

    public InvoiceResponse create(CreateInvoiceRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(request.studentId()));
        FeeStructure fee = feeStructureRepository.findById(request.feeId())
                .orElseThrow(() -> new FeeStructureNotFoundException(request.feeId()));

        validateEnrollment(student.getStudentId(), fee);
        if (invoiceRepository.existsByStudentIdAndFeeId(student.getStudentId(), fee.getFeeId())) {
            throw new InvoiceConflictException("An invoice already exists for this student and fee.");
        }
        var dueDate = request.dueDate() == null ? fee.getDueDate() : request.dueDate();
        if (dueDate == null) {
            throw new InvoiceConflictException("Provide a due date or configure one on the fee structure.");
        }
        if (dueDate.isBefore(request.issueDate())) {
            throw new InvoiceConflictException("Invoice due date cannot be before its issue date.");
        }

        Invoice invoice = new Invoice(
                generateInvoiceNumber(request.issueDate()),
                student.getStudentId(),
                fee.getFeeId(),
                fee.getSemesterId(),
                request.issueDate(),
                dueDate,
                fee.getAmount()
        );
        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    private void validateEnrollment(Integer studentId, FeeStructure fee) {
        List<ClassEnrollment> enrollments = enrollmentRepository
                .findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(studentId);
        if (fee.getClassId() != null) {
            if (!schoolClassRepository.existsById(fee.getClassId())) {
                throw new SchoolClassNotFoundException(fee.getClassId());
            }
            if (!enrollmentRepository.existsByStudentIdAndClassId(studentId, fee.getClassId())) {
                throw new InvoiceConflictException("The student must be enrolled in the fee's class.");
            }
            return;
        }

        List<Integer> classIds = enrollments.stream().map(ClassEnrollment::getClassId).distinct().toList();
        boolean enrolledInSemester = schoolClassRepository.findAllById(classIds).stream()
                .anyMatch(schoolClass -> schoolClass.getSemesterId().equals(fee.getSemesterId()));
        if (!enrolledInSemester) {
            throw new InvoiceConflictException("The student must be enrolled in a class for the fee's semester.");
        }
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private String generateInvoiceNumber(java.time.LocalDate issueDate) {
        String uniquePart = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
        return "INV-" + issueDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + uniquePart;
    }
}
