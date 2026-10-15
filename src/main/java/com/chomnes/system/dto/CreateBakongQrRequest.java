package com.chomnes.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateBakongQrRequest(
        @NotNull @Positive Integer invoiceId,
        @NotBlank @Pattern(regexp = "[A-Fa-f0-9]{32}", message = "must be a 32-character hexadecimal MD5 value")
        String md5,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amountKhr,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amountUsd,
        @NotNull @Future LocalDateTime expiresAt
) {
}
