package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateGradingCriterionRequest(
    @Positive Integer subjectId,
    @Positive Integer classId,
    @NotBlank @Size(max = 50) String componentName,
    @NotNull @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
    BigDecimal weightPercentage,
    @NotNull @Positive Integer attemptCount
) {
}
