package com.chomnes.system.service;

import com.chomnes.system.dto.CreateGradeRequest;
import com.chomnes.system.dto.GradeResponse;
import com.chomnes.system.dto.UpdateGradeRequest;
import com.chomnes.system.exception.AssessmentNotFoundException;
import com.chomnes.system.exception.GradeConflictException;
import com.chomnes.system.exception.GradeNotFoundException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.model.Assessment;
import com.chomnes.system.model.Grade;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.AssessmentRepository;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.GradeRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class GradeService {
    private final GradeRepository gradeRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public GradeService(
            GradeRepository gradeRepository,
            AssessmentRepository assessmentRepository,
            ScheduleRepository scheduleRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            ClassEnrollmentRepository enrollmentRepository
    ) {
        this.gradeRepository = gradeRepository;
        this.assessmentRepository = assessmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<GradeResponse> findAll(Integer requesterId, User.Role requesterRole) {
        return findAll(requesterId, requesterRole, null);
    }

    @Transactional(readOnly = true)
    public List<GradeResponse> findAll(Integer requesterId, User.Role requesterRole, Integer assessmentId) {
        if (requesterRole == User.Role.Admin) {
            return gradeRepository.findAllByOrderByEnteredAtDescGradeIdDesc().stream()
                    .filter(grade -> assessmentId == null || assessmentId.equals(grade.getAssessmentId()))
                    .map(GradeResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            return gradeRepository.findAllByStudentIdAndIsPublishedTrueOrderByEnteredAtDescGradeIdDesc(
                            student.getStudentId()
                    ).stream()
                    .filter(grade -> assessmentId == null || assessmentId.equals(grade.getAssessmentId()))
                    .map(GradeResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            List<Integer> scheduleIds = scheduleRepository.findAllByTeacherId(teacher.getTeacherId()).stream()
                    .map(Schedule::getScheduleId)
                    .toList();
            List<Integer> assessmentIds = scheduleIds.isEmpty()
                    ? List.of()
                    : assessmentRepository.findAllByScheduleIdInOrderByAssessmentDateAscAssessmentIdAsc(scheduleIds)
                            .stream()
                            .map(Assessment::getAssessmentId)
                            .toList();
            return gradesForAssessments(assessmentIds).stream()
                    .filter(grade -> assessmentId == null || assessmentId.equals(grade.assessmentId()))
                    .toList();
        }
        throw new AccessDeniedException("You cannot access grades.");
    }

    @Transactional(readOnly = true)
    public GradeResponse findById(Integer gradeId, Integer requesterId, User.Role requesterRole) {
        Grade grade = getGrade(gradeId);
        authorizeRead(grade, requesterId, requesterRole);
        return GradeResponse.from(grade);
    }

    public GradeResponse create(CreateGradeRequest request, Integer requesterId, User.Role requesterRole) {
        Assessment assessment = getAssessment(request.assessmentId());
        Schedule schedule = getSchedule(assessment.getScheduleId());
        Teacher enteredBy = authorizeManage(schedule, requesterId, requesterRole);
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(request.studentId()));
        if (!enrollmentRepository.existsByStudentIdAndClassId(student.getStudentId(), schedule.getClassId())) {
            throw new GradeConflictException("The student must be enrolled in the assessment's class.");
        }
        validateScore(request.score(), assessment);
        if (gradeRepository.existsByAssessmentIdAndStudentId(assessment.getAssessmentId(), student.getStudentId())) {
            throw new GradeConflictException("A grade already exists for this student and assessment.");
        }

        Grade grade = new Grade(
                assessment.getAssessmentId(),
                student.getStudentId(),
                request.score(),
                enteredBy.getTeacherId(),
                Boolean.TRUE.equals(request.isPublished())
        );
        return GradeResponse.from(gradeRepository.save(grade));
    }

    public GradeResponse update(
            Integer gradeId,
            UpdateGradeRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one grade field to update.");
        }
        Grade grade = getGrade(gradeId);
        Assessment assessment = getAssessment(grade.getAssessmentId());
        Schedule schedule = getSchedule(assessment.getScheduleId());
        authorizeManage(schedule, requesterId, requesterRole);
        if (request.score() != null) {
            validateScore(request.score(), assessment);
            grade.setScore(request.score());
        }
        if (request.isPublished() != null) {
            grade.setIsPublished(request.isPublished());
        }
        return GradeResponse.from(gradeRepository.save(grade));
    }

    private List<GradeResponse> gradesForAssessments(List<Integer> assessmentIds) {
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        return gradeRepository.findAllByAssessmentIdInOrderByEnteredAtDescGradeIdDesc(assessmentIds).stream()
                .map(GradeResponse::from)
                .toList();
    }

    public void delete(Integer gradeId) {
        getGrade(gradeId);
        gradeRepository.deleteById(gradeId);
    }

    private Grade getGrade(Integer gradeId) {
        return gradeRepository.findById(gradeId).orElseThrow(() -> new GradeNotFoundException(gradeId));
    }

    private Assessment getAssessment(Integer assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }

    private Schedule getSchedule(Integer scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    private Teacher authorizeManage(Schedule schedule, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return teacherRepository.findById(schedule.getTeacherId())
                    .orElseThrow(() -> new AccessDeniedException("The assessment schedule has no teacher profile."));
        }
        if (requesterRole != User.Role.Teacher) {
            throw new AccessDeniedException("Only an administrator or the scheduled teacher can manage grades.");
        }
        Teacher teacher = getActiveTeacherForUser(requesterId);
        if (!teacher.getTeacherId().equals(schedule.getTeacherId())) {
            throw new AccessDeniedException("Teachers can only manage grades for their own schedules.");
        }
        return teacher;
    }

    private void authorizeRead(Grade grade, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!student.getStudentId().equals(grade.getStudentId()) || !Boolean.TRUE.equals(grade.getIsPublished())) {
                throw new AccessDeniedException("Students can only access their own published grades.");
            }
            return;
        }
        if (requesterRole == User.Role.Teacher) {
            Assessment assessment = getAssessment(grade.getAssessmentId());
            authorizeManage(getSchedule(assessment.getScheduleId()), requesterId, requesterRole);
            return;
        }
        throw new AccessDeniedException("You cannot access grades.");
    }

    private void validateScore(BigDecimal score, Assessment assessment) {
        if (score.signum() < 0) {
            throw new GradeConflictException("Grade score cannot be negative.");
        }
        if (score.compareTo(assessment.getMaxScore()) > 0) {
            throw new GradeConflictException("Grade score cannot exceed the assessment's maximum score.");
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
}
