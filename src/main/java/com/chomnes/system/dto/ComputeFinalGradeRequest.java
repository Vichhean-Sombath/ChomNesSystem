package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ComputeFinalGradeRequest(
        @NotNull @Positive Integer studentId,
        @NotNull @Positive Integer classId,
        @NotNull @Positive Integer subjectId
) {
}
