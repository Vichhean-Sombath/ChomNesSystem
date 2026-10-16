package com.chomnes.system.dto;

import com.chomnes.system.model.FinalGrade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinalGradeResponse(
    Integer finalGradeId,
    Integer studentId,
    Integer subjectId,
    Integer classId,
    Integer semesterId,
    BigDecimal finalScore,
    String letterGrade,
    BigDecimal gpaPoints,
    LocalDateTime computedAt
) {
    public static FinalGradeResponse from(FinalGrade grade) {
        return new FinalGradeResponse(
            grade.getFinalGradeId(),
            grade.getStudentId(),
            grade.getSubjectId(),
            grade.getClassId(),
            grade.getSemesterId(),
            grade.getFinalScore(),
            grade.getLetterGrade(),
            grade.getGpaPoints(),
            grade.getComputedAt()
        );
    }
}
