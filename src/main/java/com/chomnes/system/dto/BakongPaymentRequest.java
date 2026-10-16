package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BakongPaymentRequest(@NotBlank @Pattern(regexp = "(?i)[a-f0-9]{32}") String md5) { }
