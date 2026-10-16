package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFinalGradeRequest(
        @NotNull @Positive Integer studentId,
        @NotNull @Positive Integer subjectId,
        @NotNull @Positive Integer classId,
        @NotNull @Positive Integer semesterId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 4, fraction = 2) BigDecimal finalScore,
        @Size(max = 2) String letterGrade,
        @DecimalMin("0.00") @Digits(integer = 1, fraction = 2) BigDecimal gpaPoints
) {
}
