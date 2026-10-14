package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateGradeRequest(
        @NotNull @Positive Integer assessmentId,
        @NotNull @Positive Integer studentId,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2)
        BigDecimal score,
        Boolean isPublished
) {
}
