package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record StudentAttendanceCheckInRequest(
        @NotNull @Positive Integer scheduleId,
        @NotNull LocalDate attendanceDate,
        @NotBlank String token
) {
}
