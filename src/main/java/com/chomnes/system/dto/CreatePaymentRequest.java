package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentRequest(
    @NotNull @Positive Integer invoiceId,
    @NotNull LocalDate paymentDate,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
    @NotBlank @Pattern(regexp = "(?i)Stripe|BakongKHQR", message = "must be Stripe or BakongKHQR")
    String paymentMethod,
    @Size(max = 255) String receiptUrl,
    @Size(max = 255) String transactionReference,
    @jakarta.validation.constraints.Positive Integer recordedBy,
    @Size(max = 255) String notes
) {
    public CreatePaymentRequest(
        Integer invoiceId,
        java.time.LocalDate paymentDate,
        BigDecimal amount,
        String paymentMethod,
        String receiptUrl,
        String transactionReference,
        String notes
    ) {
        this(invoiceId, paymentDate, amount, paymentMethod, receiptUrl, transactionReference, null, notes);
    }
}
