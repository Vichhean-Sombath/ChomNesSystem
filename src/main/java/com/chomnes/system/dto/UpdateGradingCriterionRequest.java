package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateGradingCriterionRequest(
        @Positive Integer subjectId,
        @Positive Integer classId,
        @Size(min = 1, max = 50) String componentName,
        @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal weightPercentage,
        @Positive Integer attemptCount,
        Boolean clearSubject,
        Boolean clearClass
) {
    public boolean isEmpty() {
        return subjectId == null
                && classId == null
                && componentName == null
                && weightPercentage == null
                && attemptCount == null
                && clearSubject == null
                && clearClass == null;
    }
}
