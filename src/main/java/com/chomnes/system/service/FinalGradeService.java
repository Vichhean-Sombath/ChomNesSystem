package com.chomnes.system.service;

import com.chomnes.system.dto.ComputeFinalGradeRequest;
import com.chomnes.system.dto.FinalGradeResponse;
import com.chomnes.system.exception.FinalGradeConflictException;
import com.chomnes.system.exception.FinalGradeNotFoundException;
import com.chomnes.system.exception.ScheduleNotFoundException;
import com.chomnes.system.exception.SchoolClassNotFoundException;
import com.chomnes.system.exception.StudentNotFoundException;
import com.chomnes.system.model.Assessment;
import com.chomnes.system.model.FinalGrade;
import com.chomnes.system.model.Grade;
import com.chomnes.system.model.GradingCriterion;
import com.chomnes.system.model.Schedule;
import com.chomnes.system.model.SchoolClass;
import com.chomnes.system.model.Student;
import com.chomnes.system.model.Teacher;
import com.chomnes.system.model.User;
import com.chomnes.system.repository.AssessmentRepository;
import com.chomnes.system.repository.ClassEnrollmentRepository;
import com.chomnes.system.repository.FinalGradeRepository;
import com.chomnes.system.repository.GradeRepository;
import com.chomnes.system.repository.GradingCriterionRepository;
import com.chomnes.system.repository.ScheduleRepository;
import com.chomnes.system.repository.SchoolClassRepository;
import com.chomnes.system.repository.StudentRepository;
import com.chomnes.system.repository.TeacherRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FinalGradeService {
    private final FinalGradeRepository finalGradeRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ScheduleRepository scheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final GradingCriterionRepository criterionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;

    public FinalGradeService(
            FinalGradeRepository finalGradeRepository,
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            ScheduleRepository scheduleRepository,
            AssessmentRepository assessmentRepository,
            GradeRepository gradeRepository,
            GradingCriterionRepository criterionRepository,
            ClassEnrollmentRepository enrollmentRepository,
            TeacherRepository teacherRepository
    ) {
        this.finalGradeRepository = finalGradeRepository;
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.scheduleRepository = scheduleRepository;
        this.assessmentRepository = assessmentRepository;
        this.gradeRepository = gradeRepository;
        this.criterionRepository = criterionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public List<FinalGradeResponse> findAll(Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return finalGradeRepository.findAllByOrderByComputedAtDescFinalGradeIdDesc().stream()
                    .map(FinalGradeResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            return finalGradeRepository.findAllByStudentIdOrderByComputedAtDescFinalGradeIdDesc(student.getStudentId())
                    .stream()
                    .map(FinalGradeResponse::from)
                    .toList();
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            List<Schedule> schedules = scheduleRepository.findAllByTeacherId(teacher.getTeacherId());
            List<Integer> classIds = schedules.stream()
                    .map(Schedule::getClassId)
                    .distinct()
                    .toList();
            if (classIds.isEmpty()) {
                return List.of();
            }
            return finalGradeRepository.findAllByClassIdInOrderByComputedAtDescFinalGradeIdDesc(classIds).stream()
                    .filter(finalGrade -> schedules.stream().anyMatch(schedule ->
                            schedule.getClassId().equals(finalGrade.getClassId())
                                    && schedule.getSubjectId().equals(finalGrade.getSubjectId())
                    ))
                    .map(FinalGradeResponse::from)
                    .toList();
        }
        throw new AccessDeniedException("You cannot access final grades.");
    }

    @Transactional(readOnly = true)
    public FinalGradeResponse findById(Integer finalGradeId, Integer requesterId, User.Role requesterRole) {
        FinalGrade finalGrade = getFinalGrade(finalGradeId);
        authorizeRead(finalGrade, requesterId, requesterRole);
        return FinalGradeResponse.from(finalGrade);
    }

    public FinalGradeResponse compute(
            ComputeFinalGradeRequest request,
            Integer requesterId,
            User.Role requesterRole
    ) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(request.studentId()));
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new SchoolClassNotFoundException(request.classId()));
        List<Schedule> schedules = scheduleRepository.findAllByClassIdAndSubjectId(
                schoolClass.getClassId(), request.subjectId()
        );
        authorizeManage(schedules, requesterId, requesterRole);

        if (!enrollmentRepository.existsByStudentIdAndClassId(student.getStudentId(), schoolClass.getClassId())) {
            throw new FinalGradeConflictException("The student must be enrolled in the selected class.");
        }
        if (schedules.isEmpty()) {
            throw new FinalGradeConflictException("The selected class has no schedule for this subject.");
        }

        List<Integer> scheduleIds = schedules.stream().map(Schedule::getScheduleId).toList();
        List<Assessment> assessments = assessmentRepository
                .findAllByScheduleIdInOrderByAssessmentDateAscAssessmentIdAsc(scheduleIds);
        if (assessments.isEmpty()) {
            throw new FinalGradeConflictException("No assessments are configured for this class and subject.");
        }

        List<Integer> assessmentIds = assessments.stream().map(Assessment::getAssessmentId).toList();
        List<Grade> publishedGrades = gradeRepository.findAllByAssessmentIdInAndStudentIdAndIsPublishedTrue(
                assessmentIds, student.getStudentId()
        );
        if (publishedGrades.size() != assessments.size()) {
            throw new FinalGradeConflictException("Every assessment must have a published grade before computing a final grade.");
        }

        Map<Integer, Grade> gradeByAssessmentId = new HashMap<>();
        for (Grade grade : publishedGrades) {
            gradeByAssessmentId.put(grade.getAssessmentId(), grade);
        }
        Map<Integer, GradingCriterion> criteriaById = new HashMap<>();
        for (GradingCriterion criterion : criterionRepository.findAllById(
                assessments.stream().map(Assessment::getCriteriaId).distinct().toList()
        )) {
            criteriaById.put(criterion.getCriteriaId(), criterion);
        }

        Map<Integer, BigDecimal> scoreTotalsByCriterion = new HashMap<>();
        Map<Integer, BigDecimal> maxTotalsByCriterion = new HashMap<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Assessment assessment : assessments) {
            Grade grade = gradeByAssessmentId.get(assessment.getAssessmentId());
            if (grade == null) {
                throw new FinalGradeConflictException("Every assessment must have a published grade before computing a final grade.");
            }
            if (grade.getScore().compareTo(assessment.getMaxScore()) > 0) {
                throw new FinalGradeConflictException("A published grade exceeds its assessment maximum score.");
            }
            GradingCriterion criterion = criteriaById.get(assessment.getCriteriaId());
            if (criterion == null) {
                throw new FinalGradeConflictException("An assessment refers to a missing grading criterion.");
            }
            if (!scoreTotalsByCriterion.containsKey(criterion.getCriteriaId())) {
                totalWeight = totalWeight.add(criterion.getWeightPercentage());
            }
            scoreTotalsByCriterion.merge(criterion.getCriteriaId(), grade.getScore(), BigDecimal::add);
            maxTotalsByCriterion.merge(criterion.getCriteriaId(), assessment.getMaxScore(), BigDecimal::add);
        }
        if (totalWeight.compareTo(new BigDecimal("100.00")) != 0) {
            throw new FinalGradeConflictException("Assessment grading criteria weights must total 100 percent.");
        }

        BigDecimal finalScore = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : scoreTotalsByCriterion.entrySet()) {
            GradingCriterion criterion = criteriaById.get(entry.getKey());
            BigDecimal maxTotal = maxTotalsByCriterion.get(entry.getKey());
            BigDecimal componentPercent = entry.getValue()
                    .multiply(new BigDecimal("100"))
                    .divide(maxTotal, 8, RoundingMode.HALF_UP);
            BigDecimal weightedComponent = componentPercent
                    .multiply(criterion.getWeightPercentage())
                    .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
            finalScore = finalScore.add(weightedComponent);
        }
        finalScore = finalScore.setScale(2, RoundingMode.HALF_UP);

        FinalGrade finalGrade = finalGradeRepository.findByStudentIdAndSubjectIdAndSemesterId(
                student.getStudentId(), request.subjectId(), schoolClass.getSemesterId()
        ).orElse(null);
        if (finalGrade == null) {
            finalGrade = new FinalGrade(
                    student.getStudentId(), request.subjectId(), schoolClass.getClassId(),
                    schoolClass.getSemesterId(), finalScore
            );
        }
        finalGrade.setClassId(schoolClass.getClassId());
        finalGrade.setFinalScore(finalScore);
        finalGrade.setComputedAt(LocalDateTime.now());
        return FinalGradeResponse.from(finalGradeRepository.save(finalGrade));
    }

    private FinalGrade getFinalGrade(Integer finalGradeId) {
        return finalGradeRepository.findById(finalGradeId)
                .orElseThrow(() -> new FinalGradeNotFoundException(finalGradeId));
    }

    private void authorizeRead(FinalGrade finalGrade, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole == User.Role.Student) {
            Student student = getStudentForUser(requesterId);
            if (!student.getStudentId().equals(finalGrade.getStudentId())) {
                throw new AccessDeniedException("Students can only access their own final grades.");
            }
            return;
        }
        if (requesterRole == User.Role.Teacher) {
            Teacher teacher = getActiveTeacherForUser(requesterId);
            boolean teachesSubject = scheduleRepository.findAllByTeacherId(teacher.getTeacherId()).stream()
                    .anyMatch(schedule -> schedule.getClassId().equals(finalGrade.getClassId())
                            && schedule.getSubjectId().equals(finalGrade.getSubjectId()));
            if (!teachesSubject) {
                throw new AccessDeniedException("Teachers can only access final grades for their own subjects.");
            }
            return;
        }
        throw new AccessDeniedException("You cannot access final grades.");
    }

    private void authorizeManage(List<Schedule> schedules, Integer requesterId, User.Role requesterRole) {
        if (requesterRole == User.Role.Admin) {
            return;
        }
        if (requesterRole != User.Role.Teacher) {
            throw new AccessDeniedException("Only an administrator or the scheduled teacher can compute final grades.");
        }
        Teacher teacher = getActiveTeacherForUser(requesterId);
        if (schedules.stream().noneMatch(schedule -> teacher.getTeacherId().equals(schedule.getTeacherId()))) {
            throw new AccessDeniedException("Teachers can only compute final grades for their own schedules.");
        }
    }

    private Student getStudentForUser(Integer userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No student profile is associated with this account."));
    }

    private Teacher getActiveTeacherForUser(Integer userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("No teacher profile is associated with this account."));
        if (!"Active".equals(teacher.getStatus())) {
            throw new AccessDeniedException("This teacher profile is inactive.");
        }
        return teacher;
    }
}
