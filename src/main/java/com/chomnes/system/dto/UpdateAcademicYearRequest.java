package com.chomnes.system.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateAcademicYearRequest(
        @Size(min = 1, max = 20) String yearName,
        LocalDate startDate,
        LocalDate endDate,
        @Pattern(regexp = "Active|Closed") String status
) {
    public boolean isEmpty() {
        return yearName == null && startDate == null && endDate == null && status == null;
    }
}
