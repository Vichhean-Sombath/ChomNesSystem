package com.chomnes.system.service;

import com.chomnes.system.dto.AssessmentResponse;
import com.chomnes.system.dto.CreateAssessmentRequest;
import com.chomnes.system.dto.UpdateAssessmentRequest;
import com.chomnes.system.exception.AssessmentConflictException;
import com.chomnes.system.exception.AssessmentNotFoundException;
import com.chomnes.system.exception.GradingCriterionNotFoundException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.SemesterNotFoundException;
import com.chomnes.system.model.Assessment;
import com.chomnes.system.model.ClassEnrollment;
import com.chomnes.system.model.GradingCriterion;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Semester;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.AssessmentRepository;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.GradingCriterionRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.SemesterRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final GradingCriterionRepository criterionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SemesterRepository semesterRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public AssessmentService(
        AssessmentRepository assessmentRepository,
        ScheduleRepository scheduleRepository,
        GradingCriterionRepository criterionRepository,
        SchoolClassRepository schoolClassRepository,
        SemesterRepository semesterRepository,
        TeacherRepository teacherRepository,
        StudentRepository studentRepository,
        ClassEnrollmentRepository enrollmentRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.criterionRepository = criterionRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.semesterRepository = semesterRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return assessmentRepository.findAllByOrderByAssessmentDateDescAssessmentIdDesc()
                .stream()
                .map(AssessmentResponse::from)
                .toList();
        }

        List<Integer> scheduleIds = visibleScheduleIds(requesterId, requesterRole);
        if (scheduleIds.isEmpty()) {
            return List.of();
        }
        return assessmentRepository.findAllByScheduleIdInOrderByAssessmentDateAscAssessmentIdAsc(scheduleIds)
            .stream()
            .map(AssessmentResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse findById(Integer assessmentId, Integer requesterId, User.Role requesterRole) {
        Assessment assessment = getAssessment(assessmentId);
        authorizeRead(assessment, requesterId, requesterRole);
        return AssessmentResponse.from(assessment);
    }

    public AssessmentResponse create(
        CreateAssessmentRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        Schedule schedule = getSchedule(request.scheduleId());
        authorizeManageSchedule(schedule, requesterId, requesterRole);
        validateCriterion(request.criteriaId(), schedule);
        validateAssessmentDate(schedule, request.assessmentDate());
        Assessment assessment = new Assessment(
            schedule.getScheduleId(),
            request.criteriaId(),
            requireNonBlank(request.assessmentName()),
            request.maxScore(),
            request.assessmentDate()
        );
        return AssessmentResponse.from(assessmentRepository.save(assessment));
    }

    public AssessmentResponse update(
        Integer assessmentId,
        UpdateAssessmentRequest request,
        Integer requesterId,
        User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one assessment field to update.");
        }
        if (Boolean.TRUE.equals(request.clearAssessmentDate()) && request.assessmentDate() != null) {
            throw new IllegalArgumentException("Set either assessmentDate or clearAssessmentDate, not both.");
        }

        Assessment assessment = getAssessment(assessmentId);
        Schedule currentSchedule = getSchedule(assessment.getScheduleId());
        authorizeManageSchedule(currentSchedule, requesterId, requesterRole);

        Integer scheduleId = request.scheduleId() == null ? assessment.getScheduleId() : request.scheduleId();
        if (requesterRole == User.Role.Teacher && !scheduleId.equals(assessment.getScheduleId())) {
            throw new AccessDeniedException("Teachers cannot move an assessment to another schedule.");
        }
        Schedule schedule = scheduleId.equals(assessment.getScheduleId())
            ? currentSchedule
            : getSchedule(scheduleId);
        authorizeManageSchedule(schedule, requesterId, requesterRole);

        Integer criteriaId = request.criteriaId() == null ? assessment.getCriteriaId() : request.criteriaId();
        validateCriterion(criteriaId, schedule);
        LocalDate assessmentDate = Boolean.TRUE.equals(request.clearAssessmentDate())
            ? null
            : request.assessmentDate() == null ? assessment.getAssessmentDate() : request.assessmentDate();
        validateAssessmentDate(schedule, assessmentDate);

        assessment.setScheduleId(schedule.getScheduleId());
        assessment.setCriteriaId(criteriaId);
        if (request.assessmentName() != null) {
            assessment.setAssessmentName(requireNonBlank(request.assessmentName()));
        }
        if (request.maxScore() != null) {
            assessment.setMaxScore(request.maxScore());
        }
        assessment.setAssessmentDate(assessmentDate);
        return AssessmentResponse.from(assessmentRepository.save(assessment));
    }

    public void delete(Integer assessmentId) {
        getAssessment(assessmentId);
        assessmentRepository.deleteById(assessmentId);
    }

    private Assessment getAssessment(Integer assessmentId) {
        return assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }

    private Schedule getSchedule(Integer scheduleId) {
        return scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    private List<Integer> visibleScheduleIds(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            return scheduleRepository.findAllByTeacherId(teacher.getTeacherId())
                .stream()
                .map(Schedule::getScheduleId)
                .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            List<Integer> classIds = enrollmentRepository
                .findAllByStudentIdOrderByEnrollmentDateDescEnrollmentIdAsc(student.getStudentId())
                .stream()
                .map(ClassEnrollment::getClassId)
                .distinct()
                .toList();
            if (classIds.isEmpty()) {
                return List.of();
            }
            return scheduleRepository.findAllByClassIdIn(classIds)
                .stream()
                .map(Schedule::getScheduleId)
                .toList();
        }
        throw new AccessDeniedException("You cannot access assessments.");
    }

    private void authorizeRead(Assessment assessment, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        Schedule schedule = getSchedule(assessment.getScheduleId());
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            if (!teacher.getTeacherId().equals(schedule.getTeacherId())) {
                throw new AccessDeniedException("Teachers can only access assessments for their own schedules.");
            }
            return;
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!enrollmentRepository.existsByStudentIdAndClassId(
                student.getStudentId(), schedule.getClassId()
            )) {
                throw new AccessDeniedException("Students can only access assessments for their enrolled classes.");
            }
            return;
        }
        throw new AccessDeniedException("You cannot access assessments.");
    }

    private void authorizeManageSchedule(Schedule schedule, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole != User.Role.Teacher) {
            throw new AccessDeniedException("Only an administrator or the scheduled teacher can manage assessments.");
        }
        Teacher teacher = getActiveTeacherForUser(requesterId);
        if (!teacher.getTeacherId().equals(schedule.getTeacherId())) {
            throw new AccessDeniedException("Teachers can only manage assessments for their own schedules.");
        }
    }

    private void validateCriterion(Integer criteriaId, Schedule schedule) {
        GradingCriterion criterion = criterionRepository.findById(criteriaId)
            .orElseThrow(() -> new GradingCriterionNotFoundException(criteriaId));
        if (criterion.getSubjectId() != null && !criterion.getSubjectId().equals(schedule.getSubjectId())) {
            throw new AssessmentConflictException("The grading criterion is not configured for this subject.");
        }
        if (criterion.getClassId() != null && !criterion.getClassId().equals(schedule.getClassId())) {
            throw new AssessmentConflictException("The grading criterion is not configured for this class.");
        }
    }

    private void validateAssessmentDate(Schedule schedule, LocalDate assessmentDate) {
        if (assessmentDate == null) {
            return;
        }
        SchoolClass schoolClass = schoolClassRepository.findById(schedule.getClassId())
            .orElseThrow(() -> new SchoolClassNotFoundException(schedule.getClassId()));
        Semester semester = semesterRepository.findById(schoolClass.getSemesterId())
            .orElseThrow(() -> new SemesterNotFoundException(schoolClass.getSemesterId()));
        if (assessmentDate.isBefore(semester.getStartDate()) || assessmentDate.isAfter(semester.getEndDate())) {
            throw new AssessmentConflictException("Assessment date must fall within the class semester.");
        }
    }

    private Teacher getActiveTeacherForUser(Integer userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
            .orElseThrow(() -> new AccessDeniedException("No teacher profile is associated with this account."));
        if (!"Active".equals(teacher.getStatus())) {
            throw new AccessDeniedException("This teacher profile is inactive.");
        }
        return teacher;
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
            .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private String requireNonBlank(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Assessment name must not be blank.");
        }
        return trimmed;
    }
}
