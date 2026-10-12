package com.chomnes.system.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateSemesterRequest(
        @Positive Integer academicYearId,
        @Size(min = 1, max = 30) String semesterName,
        LocalDate startDate,
        LocalDate endDate
) {
    public boolean isEmpty() {
        return academicYearId == null
                && semesterName == null
                && startDate == null
                && endDate == null;
    }
}
