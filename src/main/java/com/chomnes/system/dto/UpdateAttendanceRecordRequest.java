package com.chomnes.system.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdateAttendanceRecordRequest(
    LocalDate attendanceDate,
    @Pattern(regexp = "Present|Absent|Late|Excused") String status
) {
    public boolean isEmpty() {
        return attendanceDate == null && status == null;
    }
}
