package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCertificateRequest(
    @NotNull @Positive Integer studentId,
    @NotNull @Pattern(
        regexp = "Completion|Transcript|Recommendation",
        message = "must be Completion, Transcript, or Recommendation"
    ) String certificateType,
    @Size(max = 100) String templateUsed,
    @NotNull LocalDate issueDate,
    @Size(max = 255) String generatedFileUrl
) {
}
