package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInvoiceRequest(
    @Size(max = 30) String invoiceNumber,
    @Positive Integer studentId,
    @Positive Integer feeId,
    @Positive Integer semesterId,
    LocalDate issueDate,
    LocalDate dueDate,
    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal totalAmount,
    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal amountPaid,
    @Pattern(regexp = "Unpaid|Partial|Paid|Overdue") String status
) {
    public boolean isEmpty() {
        return invoiceNumber == null && studentId == null && feeId == null && semesterId == null
            && issueDate == null && dueDate == null && totalAmount == null && amountPaid == null && status == null;
    }
}
