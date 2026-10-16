package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvoiceRequest(
        @NotNull @Positive Integer studentId,
        @NotNull @Positive Integer feeId,
        @NotNull LocalDate issueDate,
        LocalDate dueDate,
        @Size(max = 30) String invoiceNumber,
        @Positive Integer semesterId,
        @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal totalAmount,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal amountPaid,
        @Pattern(regexp = "Unpaid|Partial|Paid|Overdue") String status
) {
    public CreateInvoiceRequest(Integer studentId, Integer feeId, LocalDate issueDate, LocalDate dueDate) {
        this(studentId, feeId, issueDate, dueDate, null, null, null, null, null);
    }
}
