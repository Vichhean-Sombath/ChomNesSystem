package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StripeCheckoutRequest(
    @NotNull @Positive Integer invoiceId,
    @Size(max = 2048) String successUrl,
    @Size(max = 2048) String cancelUrl
) {
}
