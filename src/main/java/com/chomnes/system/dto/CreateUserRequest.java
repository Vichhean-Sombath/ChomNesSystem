package com.chomnes.system.dto;

import com.chomnes.system.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull User.Role role,
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        @Past LocalDate dob,
        @Pattern(regexp = "Male|Female|Other") String gender,
        @Size(max = 255) String photoUrl,
        @Size(max = 20) String contactNumber,
        @Size(max = 255) String address,
        @Size(max = 100) String specialization,
        String bio,
        LocalDate enrollmentDate
) {
    public CreateUserRequest(String username, String email, String password, User.Role role) {
        this(username, email, password, role, null, null, null, null, null, null, null, null, null, null);
    }
}
