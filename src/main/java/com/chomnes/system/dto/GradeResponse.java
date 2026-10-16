package com.chomnes.system.dto;

import com.chomnes.system.model.Grade;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeResponse(
    Integer gradeId,
    Integer assessmentId,
    Integer studentId,
    BigDecimal score,
    Integer enteredBy,
    LocalDateTime enteredAt,
    Boolean isPublished
) {
    public static GradeResponse from(Grade grade) {
        return new GradeResponse(
            grade.getGradeId(),
            grade.getAssessmentId(),
            grade.getStudentId(),
            grade.getScore(),
            grade.getEnteredBy(),
            grade.getEnteredAt(),
            grade.getIsPublished()
        );
    }
}
