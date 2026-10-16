package com.chomnes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTeacherRequest(
    @NotNull @Positive Integer userId,
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @Past LocalDate dob,
    @Pattern(regexp = "Male|Female|Other") String gender,
    @Size(max = 255) String photoUrl,
    @Size(max = 20) String contactNumber,
    @Size(max = 100) String specialization,
    String bio,
    LocalDate hireDate
) {
}
