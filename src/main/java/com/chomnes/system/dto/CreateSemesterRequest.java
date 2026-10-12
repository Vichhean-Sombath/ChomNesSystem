package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSemesterRequest(
        @NotNull @Positive Integer academicYearId,
        @NotBlank @Size(max = 30) String semesterName,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
