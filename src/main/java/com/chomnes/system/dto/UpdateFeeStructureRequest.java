package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFeeStructureRequest(
    @Size(max = 100) String feeName,
    @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
    LocalDate dueDate,
    Boolean clearDueDate
) {
    public boolean isEmpty() {
        return feeName == null && amount == null && dueDate == null && clearDueDate == null;
    }
}
