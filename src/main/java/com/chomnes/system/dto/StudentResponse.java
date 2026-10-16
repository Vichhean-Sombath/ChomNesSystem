package com.chomnes.system.dto;

import com.chomnes.system.model.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
    Integer studentId,
    Integer userId,
    String firstName,
    String lastName,
    LocalDate dob,
    String gender,
    String photoUrl,
    String contactNumber,
    String address,
    LocalDate enrollmentDate,
    String status,
    LocalDateTime createdAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
            student.getStudentId(),
            student.getUserId(),
            student.getFirstName(),
            student.getLastName(),
            student.getDob(),
            student.getGender(),
            student.getPhotoUrl(),
            student.getContactNumber(),
            student.getAddress(),
            student.getEnrollmentDate(),
            student.getStatus(),
            student.getCreatedAt()
        );
    }
}
