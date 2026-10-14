package com.chomnes.system.repository;

import com.chomnes.system.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Integer> {
    boolean existsByAssessmentIdAndStudentId(Integer assessmentId, Integer studentId);

    boolean existsByAssessmentIdAndStudentIdAndGradeIdNot(Integer assessmentId, Integer studentId, Integer gradeId);

    List<Grade> findAllByOrderByEnteredAtDescGradeIdDesc();

    List<Grade> findAllByStudentIdAndIsPublishedTrueOrderByEnteredAtDescGradeIdDesc(Integer studentId);

    List<Grade> findAllByAssessmentIdInOrderByEnteredAtDescGradeIdDesc(List<Integer> assessmentIds);
}
