package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePaymentRequest(
    @Positive Integer invoiceId,
    LocalDate paymentDate,
    @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
    @Pattern(regexp = "(?i)Stripe|BakongKHQR") String paymentMethod,
    @Size(max = 255) String receiptUrl,
    @Size(max = 255) String transactionReference,
    @Positive Integer recordedBy,
    @Size(max = 255) String notes
) {
    public boolean isEmpty() {
        return invoiceId == null && paymentDate == null && amount == null && paymentMethod == null
            && receiptUrl == null && transactionReference == null && recordedBy == null && notes == null;
    }
}
