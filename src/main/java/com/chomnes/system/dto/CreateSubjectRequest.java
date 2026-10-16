package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
    @NotBlank @Size(max = 20) String subjectCode,
    @NotBlank @Size(max = 100) String subjectName,
    String description,
    @Positive Integer prerequisiteSubjectId
) {
}
