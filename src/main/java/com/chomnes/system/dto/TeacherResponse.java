package com.chomnes.system.dto;

import com.chomnes.system.model.Teacher;

import java.time.LocalDate;

public record TeacherResponse(
        Integer teacherId,
        Integer userId,
        String firstName,
        String lastName,
        LocalDate dob,
        String gender,
        String photoUrl,
        String contactNumber,
        String specialization,
        String bio,
        LocalDate hireDate,
        String status
) {
    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getTeacherId(),
                teacher.getUserId(),
                teacher.getFirstName(),
                teacher.getLastName(),
                teacher.getDob(),
                teacher.getGender(),
                teacher.getPhotoUrl(),
                teacher.getContactNumber(),
                teacher.getSpecialization(),
                teacher.getBio(),
                teacher.getHireDate(),
                teacher.getStatus()
        );
    }
}
