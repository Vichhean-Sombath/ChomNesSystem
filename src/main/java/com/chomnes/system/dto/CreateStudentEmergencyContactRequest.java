package com.chomnes.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateStudentEmergencyContactRequest(
    @Positive Integer studentId,
    @NotBlank @Size(max = 100) String contactName,
    @Size(max = 50) String relationship,
    @NotBlank @Size(max = 20) String phoneNumber,
    @Email @Size(max = 100) String email
) {
}
