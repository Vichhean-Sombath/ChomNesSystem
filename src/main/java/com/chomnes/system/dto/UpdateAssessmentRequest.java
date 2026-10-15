package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateAssessmentRequest(
        @Positive Integer scheduleId,
        @Positive Integer criteriaId,
        @Size(max = 100) String assessmentName,
        @DecimalMin("0.01") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2)
        BigDecimal maxScore,
        LocalDate assessmentDate,
        Boolean clearAssessmentDate
) {
    public boolean isEmpty() {
        return scheduleId == null
                && criteriaId == null
                && assessmentName == null
                && maxScore == null
                && assessmentDate == null
                && clearAssessmentDate == null;
    }
}
