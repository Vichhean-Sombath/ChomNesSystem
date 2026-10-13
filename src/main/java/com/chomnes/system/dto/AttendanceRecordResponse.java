package com.chomnes.system.dto;

import com.chomnes.system.model.AttendanceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceRecordResponse(
        Integer attendanceId,
        Integer scheduleId,
        Integer studentId,
        LocalDate attendanceDate,
        String status,
        Integer markedBy,
        LocalDateTime markedAt,
        LocalDateTime lastEditedAt
) {
    public static AttendanceRecordResponse from(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getAttendanceId(),
                record.getScheduleId(),
                record.getStudentId(),
                record.getAttendanceDate(),
                record.getStatus(),
                record.getMarkedBy(),
                record.getMarkedAt(),
                record.getLastEditedAt()
        );
    }
}
