package com.chomnes.system.service;

import com.chomnes.system.dto.CreateSchoolClassRequest;
import com.chomnes.system.dto.SchoolClassResponse;
import com.chomnes.system.dto.UpdateSchoolClassRequest;
import com.chomnes.system.exception.AcademicYearConflictException;
import com.chomnes.system.exception.AcademicYearNotFoundException;
import com.chomnes.system.exception.SchoolClassConflictException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.exception.TeacherNotFoundException;
import com.chomnes.system.model.AcademicYear;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.repository.AcademicYearRepository;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SchoolClassService {
    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;

    public SchoolClassService(
            SchoolClassRepository schoolClassRepository,
            AcademicYearRepository academicYearRepository,
            SemesterRepository semesterRepository,
            TeacherRepository teacherRepository,
            ClassEnrollmentRepository classEnrollmentRepository
    ) {
        this.schoolClassRepository = schoolClassRepository;
        this.academicYearRepository = academicYearRepository;
        this.semesterRepository = semesterRepository;
        this.teacherRepository = teacherRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> findAll() {
        return schoolClassRepository.findAll(Sort.by(Sort.Direction.ASC, "classId"))
                .stream()
                .map(SchoolClassResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolClassResponse findById(Integer classId) {
        return SchoolClassResponse.from(getSchoolClass(classId));
    }

    public SchoolClassResponse create(CreateSchoolClassRequest request) {
        String className = requireNonBlank(request.className(), "Class name");
        validateAcademicPeriod(request.academicYearId(), request.semesterId());
        validateHomeroomTeacher(request.homeroomTeacherId());

        SchoolClass schoolClass = new SchoolClass(
                className,
                request.academicYearId(),
                request.semesterId(),
                normalizeOptional(request.roomNumber()),
                request.maxCapacity(),
                request.homeroomTeacherId()
        );
        return SchoolClassResponse.from(schoolClassRepository.save(schoolClass));
    }

    public SchoolClassResponse update(Integer classId, UpdateSchoolClassRequest request) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one class field to update.");
        }
        if (Boolean.TRUE.equals(request.clearRoomNumber()) && request.roomNumber() != null) {
            throw new IllegalArgumentException("Set either roomNumber or clearRoomNumber, not both.");
        }
        if (Boolean.TRUE.equals(request.clearHomeroomTeacher()) && request.homeroomTeacherId() != null) {
            throw new IllegalArgumentException("Set either homeroomTeacherId or clearHomeroomTeacher, not both.");
        }

        SchoolClass schoolClass = getSchoolClass(classId);
        Integer academicYearId = request.academicYearId() == null
                ? schoolClass.getAcademicYearId()
                : request.academicYearId();
        Integer semesterId = request.semesterId() == null ? schoolClass.getSemesterId() : request.semesterId();
        validateAcademicPeriod(academicYearId, semesterId);

        Integer teacherId = Boolean.TRUE.equals(request.clearHomeroomTeacher())
                ? null
                : request.homeroomTeacherId() == null
                        ? schoolClass.getHomeroomTeacherId()
                        : request.homeroomTeacherId();
        validateHomeroomTeacher(teacherId);

        String className = request.className() == null
                ? schoolClass.getClassName()
                : requireNonBlank(request.className(), "Class name");
        Integer maxCapacity = request.maxCapacity() == null ? schoolClass.getMaxCapacity() : request.maxCapacity();
        if (maxCapacity != null) {
            long activeEnrollments = classEnrollmentRepository.countByClassIdAndStatus(classId, "Active");
            if (maxCapacity < activeEnrollments) {
                throw new SchoolClassConflictException("Maximum capacity cannot be lower than active enrollments.");
            }
        }

        schoolClass.setClassName(className);
        schoolClass.setAcademicYearId(academicYearId);
        schoolClass.setSemesterId(semesterId);
        schoolClass.setMaxCapacity(maxCapacity);
        schoolClass.setHomeroomTeacherId(teacherId);
        if (Boolean.TRUE.equals(request.clearRoomNumber())) {
            schoolClass.setRoomNumber(null);
        } else if (request.roomNumber() != null) {
            schoolClass.setRoomNumber(normalizeOptional(request.roomNumber()));
        }
        return SchoolClassResponse.from(schoolClassRepository.save(schoolClass));
    }

    private SchoolClass getSchoolClass(Integer classId) {
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> new SchoolClassNotFoundException(classId));
    }

    private void validateAcademicPeriod(Integer academicYearId, Integer semesterId) {
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new AcademicYearNotFoundException(academicYearId));
        if (!"Active".equals(academicYear.getStatus())) {
            throw new AcademicYearConflictException("Classes can only be assigned to an active academic year.");
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new SemesterNotFoundException(semesterId));
        if (!semester.getAcademicYearId().equals(academicYearId)) {
            throw new SchoolClassConflictException("The semester must belong to the selected academic year.");
        }
        if (semester.getStartDate().isBefore(academicYear.getStartDate())
                || semester.getEndDate().isAfter(academicYear.getEndDate())) {
            throw new SchoolClassConflictException("The semester dates fall outside the academic year.");
        }
    }

    private void validateHomeroomTeacher(Integer teacherId) {
        if (teacherId == null) {
            return;
        }
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new TeacherNotFoundException(teacherId));
        if (!"Active".equals(teacher.getStatus())) {
            throw new SchoolClassConflictException("An inactive teacher cannot be assigned as homeroom teacher.");
        }
    }

    private String requireNonBlank(String value, String field) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
