package com.chomnes.system.service;

import com.chomnes.system.dto.ClassEnrollmentResponse;
import com.chomnes.system.dto.CreateClassEnrollmentRequest;
import com.chomnes.system.dto.UpdateClassEnrollmentRequest;
import com.chomnes.system.exception.AcademicYearConflictException;
import com.chomnes.system.exception.AcademicYearNotFoundException;
import com.chomnes.system.exception.ClassEnrollmentConflictException;
import com.chomnes.system.exception.ClassEnrollmentNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.model.AcademicYear;
import com.chomnes.system.model.ClassEnrollment;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.AcademicYearRepository;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.StudentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class ClassEnrollmentService {
    private final ClassEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    public ClassEnrollmentService(
            ClassEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentResponse> findAll(Integer requesterId, User.Role requesterRole) {
        List<ClassEnrollment> enrollments;
        if (requesterRole == User.Role.Admin || requesterRole == User.Role.Teacher) {
            enrollments = enrollmentRepository.findAllByOrderByEnrollmentDateDescEnrollmentIdAsc();
        } else if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if ("Inactive".equals(student.getStatus())) {
                throw new AccessDeniedException("This student profile is inactive.");
            }
            enrollments = enrollmentRepository.findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(
                    student.getStudentId()
            );
        } else {
            throw new AccessDeniedException("You cannot access class enrollments.");
        }
        return enrollments.stream().map(ClassEnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClassEnrollmentResponse findById(Integer enrollmentId, Integer requesterId, User.Role requesterRole) {
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        authorizeRead(enrollment, requesterId, requesterRole);
        return ClassEnrollmentResponse.from(enrollment);
    }

    public ClassEnrollmentResponse create(CreateClassEnrollmentRequest request) {
        Student student = getStudent(request.studentId());
        if (!"Active".equals(student.getStatus())) {
            throw new ClassEnrollmentConflictException("An inactive student cannot be enrolled in a class.");
        }

        SchoolClass schoolClass = getActiveClass(request.classId());
        if (enrollmentRepository.existsByStudentIdAndClassIdAndStatus(
                student.getStudentId(), schoolClass.getClassId(), "Active")) {
            throw new ClassEnrollmentConflictException("The student is already actively enrolled in this class.");
        }
        requireAvailableCapacity(schoolClass);

        LocalDate enrollmentDate = request.enrollmentDate() == null ? LocalDate.now() : request.enrollmentDate();
        ClassEnrollment enrollment = new ClassEnrollment(student.getStudentId(), schoolClass.getClassId(), enrollmentDate);
        return ClassEnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    public ClassEnrollmentResponse update(
            Integer enrollmentId,
            UpdateClassEnrollmentRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one class enrollment field to update.");
        }
        ClassEnrollment enrollment = getEnrollment(enrollmentId);
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!student.getStudentId().equals(enrollment.getStudentId())) {
                throw new AccessDeniedException("Students can only update their own enrollments.");
            }
            if (request.enrollmentDate() != null || !"Withdrawn".equals(request.status())) {
                throw new AccessDeniedException("Students may only withdraw from their own active enrollment.");
            }
            if (!"Active".equals(enrollment.getStatus()) || !"Active".equals(student.getStatus())) {
                throw new ClassEnrollmentConflictException("Only an active student can withdraw from an active enrollment.");
            }
        } else if (requesterRole != User.Role.Admin) {
            throw new AccessDeniedException("Only an administrator or the enrolled student can update this enrollment.");
        }

        String nextStatus = request.status() == null ? enrollment.getStatus() : request.status();
        LocalDate nextEnrollmentDate = request.enrollmentDate() == null
                ? enrollment.getEnrollmentDate()
                : request.enrollmentDate();
        if ("Active".equals(nextStatus) && !"Active".equals(enrollment.getStatus())) {
            Student student = getStudent(enrollment.getStudentId());
            if (!"Active".equals(student.getStatus())) {
                throw new ClassEnrollmentConflictException("An inactive student cannot be enrolled in a class.");
            }
            SchoolClass schoolClass = getActiveClass(enrollment.getClassId());
            if (enrollmentRepository.existsByStudentIdAndClassIdAndStatusAndEnrollmentIdNot(
                    enrollment.getStudentId(), enrollment.getClassId(), "Active", enrollmentId)) {
                throw new ClassEnrollmentConflictException("The student is already actively enrolled in this class.");
            }
            requireAvailableCapacity(schoolClass);
        }

        enrollment.setEnrollmentDate(nextEnrollmentDate);
        enrollment.setStatus(nextStatus);
        return ClassEnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    private void authorizeRead(ClassEnrollment enrollment, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin || requesterRole == User.Role.Teacher) {
            return;
        }
        if (requesterRole != User.Role.Student) {
            throw new AccessDeniedException("You cannot access class enrollments.");
        }
        Student student = getStudentForUser(requesterId);
        if (!student.getStudentId().equals(enrollment.getStudentId())) {
            throw new AccessDeniedException("Students can only access their own enrollments.");
        }
        if (!"Active".equals(student.getStatus())) {
            throw new AccessDeniedException("This student profile is inactive.");
        }
    }

    private ClassEnrollment getEnrollment(Integer enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ClassEnrollmentNotFoundException(enrollmentId));
    }

    private Student getStudent(Integer studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private SchoolClass getActiveClass(Integer classId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new SchoolClassNotFoundException(classId));
        AcademicYear academicYear = academicYearRepository.findById(schoolClass.getAcademicYearId())
                .orElseThrow(() -> new AcademicYearNotFoundException(schoolClass.getAcademicYearId()));
        if (!"Active".equals(academicYear.getStatus())) {
            throw new AcademicYearConflictException("Students cannot enroll in a class from a closed academic year.");
        }
        Semester semester = semesterRepository.findById(schoolClass.getSemesterId())
                .orElseThrow(() -> new SemesterNotFoundException(schoolClass.getSemesterId()));
        if (!semester.getAcademicYearId().equals(academicYear.getAcademicYearId())) {
            throw new ClassEnrollmentConflictException("The class semester does not belong to its academic year.");
        }
        return schoolClass;
    }

    private void requireAvailableCapacity(SchoolClass schoolClass) {
        Integer capacity = schoolClass.getMaxCapacity();
        if (capacity == null) {
            return;
        }
        long activeEnrollments = enrollmentRepository.countByClassIdAndStatus(schoolClass.getClassId(), "Active");
        if (activeEnrollments >= capacity) {
            throw new ClassEnrollmentConflictException("This class has reached its maximum capacity.");
        }
    }
}
