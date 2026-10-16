package com.chomnes.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateAttendanceRecordRequest(
    @NotNull @Positive Integer scheduleId,
    @NotNull @Positive Integer studentId,
    LocalDate attendanceDate,
    @NotNull @Pattern(regexp = "Present|Absent|Late|Excused") String status
) {
}
