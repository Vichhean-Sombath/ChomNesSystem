package com.chomnes.system.service;

import com.chomnes.system.dto.CreateFeeStructureRequest;
import com.chomnes.system.dto.FeeStructureResponse;
import com.chomnes.system.dto.UpdateFeeStructureRequest;
import com.chomnes.system.exception.FeeStructureConflictException;
import com.chomnes.system.exception.FeeStructureNotFoundException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.model.ClassEnrollment;
import com.chomnes.system.model.FeeStructure;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.FeeStructureRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FeeStructureService {
    private final FeeStructureRepository feeStructureRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;

    public FeeStructureService(
            FeeStructureRepository feeStructureRepository,
            SchoolClassRepository schoolClassRepository,
            SemesterRepository semesterRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            ClassEnrollmentRepository enrollmentRepository,
            ScheduleRepository scheduleRepository
    ) {
        this.feeStructureRepository = feeStructureRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> findAll(Integer requesterId, User.Role requesterRole) {
        List<FeeStructure> fees = feeStructureRepository.findAllByOrderBySemesterIdAscFeeIdAsc();
        if (requesterRole == User.Role.Admin) {
            return fees.stream().map(FeeStructureResponse::from).toList();
        }
        Scope scope = getScope(requesterId, requesterRole);
        return fees.stream()
                .filter(fee -> isVisible(fee, scope))
                .map(FeeStructureResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse findById(Integer feeId, Integer requesterId, User.Role requesterRole) {
        FeeStructure fee = getFeeStructure(feeId);
        if (requesterRole != User.Role.Admin && !isVisible(fee, getScope(requesterId, requesterRole))) {
            throw new AccessDeniedException("You cannot access this fee structure.");
        }
        return FeeStructureResponse.from(fee);
    }

    public FeeStructureResponse create(CreateFeeStructureRequest request) {
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new SemesterNotFoundException(request.semesterId()));
        Integer classId = request.classId();
        if (classId != null) {
            SchoolClass schoolClass = schoolClassRepository.findById(classId)
                    .orElseThrow(() -> new SchoolClassNotFoundException(classId));
            if (!schoolClass.getSemesterId().equals(semester.getSemesterId())) {
                throw new FeeStructureConflictException("The fee semester must match the selected class semester.");
            }
        }
        FeeStructure feeStructure = new FeeStructure(
                classId,
                semester.getSemesterId(),
                requireNonBlank(request.feeName()),
                request.amount(),
                request.dueDate()
        );
        return FeeStructureResponse.from(feeStructureRepository.save(feeStructure));
    }

    public FeeStructureResponse update(Integer feeId, UpdateFeeStructureRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one fee structure field to update.");
        }
        if (Boolean.TRUE.equals(request.clearDueDate()) && request.dueDate() != null) {
            throw new IllegalArgumentException("Set either dueDate or clearDueDate, not both.");
        }
        FeeStructure feeStructure = getFeeStructure(feeId);
        if (request.feeName() != null) {
            feeStructure.setFeeName(requireNonBlank(request.feeName()));
        }
        if (request.amount() != null) {
            feeStructure.setAmount(request.amount());
        }
        if (Boolean.TRUE.equals(request.clearDueDate())) {
            feeStructure.setDueDate(null);
        } else if (request.dueDate() != null) {
            feeStructure.setDueDate(request.dueDate());
        }
        return FeeStructureResponse.from(feeStructureRepository.save(feeStructure));
    }

    private FeeStructure getFeeStructure(Integer feeId) {
        return feeStructureRepository.findById(feeId)
                .orElseThrow(() -> new FeeStructureNotFoundException(feeId));
    }

    private Scope getScope(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Student) {
            Student student = studentRepository.findByUserId(requesterId)
                    .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
            List<Integer> classIds = enrollmentRepository
                    .findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(student.getStudentId()).stream()
                    .map(ClassEnrollment::getClassId)
                    .distinct()
                    .toList();
            List<Integer> semesterIds = schoolClassRepository.findAllById(classIds).stream()
                    .map(SchoolClass::getSemesterId)
                    .distinct()
                    .toList();
            return new Scope(classIds, semesterIds);
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = teacherRepository.findByUserId(requesterId)
                    .orElseThrow(() -> new AccessDeniedException("No teacher profile is associated with this account."));
            if (!"Active".equals(teacher.getStatus())) {
                throw new AccessDeniedException("This teacher profile is inactive.");
            }
            List<Integer> classIds = scheduleRepository.findAllByTeacherId(teacher.getTeacherId()).stream()
                    .map(Schedule::getClassId)
                    .distinct()
                    .toList();
            List<Integer> semesterIds = schoolClassRepository.findAllById(classIds).stream()
                    .map(SchoolClass::getSemesterId)
                    .distinct()
                    .toList();
            return new Scope(classIds, semesterIds);
        }
        throw new AccessDeniedException("You cannot access fee structures.");
    }

    private boolean isVisible(FeeStructure feeStructure, Scope scope) {
        return scope.semesterIds().contains(feeStructure.getSemesterId())
                && (feeStructure.getClassId() == null || scope.classIds().contains(feeStructure.getClassId()));
    }

    private String requireNonBlank(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Fee name must not be blank.");
        }
        return trimmed;
    }

    private record Scope(List<Integer> classIds, List<Integer> semesterIds) {
    }
}
