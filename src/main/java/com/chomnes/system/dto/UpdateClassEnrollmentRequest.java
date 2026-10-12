package com.chomnes.system.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdateClassEnrollmentRequest(
        LocalDate enrollmentDate,
        @Pattern(regexp = "Active|Transferred|Withdrawn") String status
) {
    public boolean isEmpty() {
        return enrollmentDate == null && status == null;
    }
}
