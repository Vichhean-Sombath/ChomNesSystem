package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAssessmentRequest(
        @NotNull @Positive Integer scheduleId,
        @NotNull @Positive Integer criteriaId,
        @NotBlank @Size(max = 100) String assessmentName,
        @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2)
        BigDecimal maxScore,
        LocalDate assessmentDate
) {
}
