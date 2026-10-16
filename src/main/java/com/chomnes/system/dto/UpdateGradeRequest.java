package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record UpdateGradeRequest(
    @DecimalMin("0.00") @DecimalMax("9999.99") @Digits(integer = 4, fraction = 2)
    BigDecimal score,
    Boolean isPublished
) {
    public boolean isEmpty() {
        return score == null && isPublished == null;
    }
}
