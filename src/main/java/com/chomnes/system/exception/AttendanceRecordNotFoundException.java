package com.chomnes.system.exception;

public class AttendanceRecordNotFoundException extends RuntimeException {
    public AttendanceRecordNotFoundException(Integer attendanceId) {
        super("Attendance record " + attendanceId + " was not found.");
    }
}
