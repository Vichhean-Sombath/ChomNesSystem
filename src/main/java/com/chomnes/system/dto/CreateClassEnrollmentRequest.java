package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateClassEnrollmentRequest(
        @NotNull @Positive Integer studentId,
        @NotNull @Positive Integer classId,
        LocalDate enrollmentDate
) {
}
