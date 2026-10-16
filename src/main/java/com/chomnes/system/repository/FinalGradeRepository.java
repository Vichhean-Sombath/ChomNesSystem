package com.chomnes.system.repository;

import com.chomnes.system.model.FinalGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinalGradeRepository extends JpaRepository<FinalGrade, Integer> {
    Optional<FinalGrade> findByStudentIdAndSubjectIdAndSemesterId(
        Integer studentId,
        Integer subjectId,
        Integer semesterId
    );

    boolean existsByStudentIdAndSubjectIdAndSemesterIdAndFinalGradeIdNot(
        Integer studentId,
        Integer subjectId,
        Integer semesterId,
        Integer finalGradeId
    );

    List<FinalGrade> findAllByOrderByComputedAtDescFinalGradeIdDesc();

    List<FinalGrade> findAllByStudentIdOrderByComputedAtDescFinalGradeIdDesc(Integer studentId);

    List<FinalGrade> findAllByClassIdInOrderByComputedAtDescFinalGradeIdDesc(List<Integer> classIds);
}
