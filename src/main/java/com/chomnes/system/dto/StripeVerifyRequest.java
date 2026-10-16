package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StripeVerifyRequest(@NotBlank @Size(max = 255) String sessionId) { }
