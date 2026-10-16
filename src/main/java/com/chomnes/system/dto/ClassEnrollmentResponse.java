package com.chomnes.system.dto;

import com.chomnes.system.model.ClassEnrollment;

import java.time.LocalDate;

public record ClassEnrollmentResponse(
    Integer enrollmentId,
    Integer studentId,
    Integer classId,
    LocalDate enrollmentDate,
    String status
) {
    public static ClassEnrollmentResponse from(ClassEnrollment enrollment) {
        return new ClassEnrollmentResponse(
            enrollment.getEnrollmentId(),
            enrollment.getStudentId(),
            enrollment.getClassId(),
            enrollment.getEnrollmentDate(),
            enrollment.getStatus()
        );
    }
}
