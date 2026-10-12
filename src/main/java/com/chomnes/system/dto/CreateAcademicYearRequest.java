package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateAcademicYearRequest(
        @NotBlank @Size(max = 20) String yearName,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
