package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateFinalGradeRequest(
    @Positive Integer studentId,
    @Positive Integer subjectId,
    @Positive Integer classId,
    @Positive Integer semesterId,
    @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal finalScore,
    @Size(max = 2) String letterGrade,
    @DecimalMin("0.00") @Digits(integer = 1, fraction = 2) BigDecimal gpaPoints
) {
    public boolean isEmpty() {
        return studentId == null && subjectId == null && classId == null && semesterId == null
            && finalScore == null && letterGrade == null && gpaPoints == null;
    }
}
