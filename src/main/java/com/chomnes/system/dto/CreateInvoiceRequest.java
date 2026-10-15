package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateInvoiceRequest(
        @NotNull @Positive Integer studentId,
        @NotNull @Positive Integer feeId,
        @NotNull LocalDate issueDate,
        LocalDate dueDate
) {
}
