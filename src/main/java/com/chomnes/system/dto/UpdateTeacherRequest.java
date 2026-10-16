package com.chomnes.system.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTeacherRequest(
    @Size(min = 1, max = 50) String firstName,
    @Size(min = 1, max = 50) String lastName,
    @Past LocalDate dob,
    @Pattern(regexp = "Male|Female|Other") String gender,
    @Size(max = 255) String photoUrl,
    @Size(max = 20) String contactNumber,
    @Size(max = 100) String specialization,
    String bio,
    LocalDate hireDate,
    @Pattern(regexp = "Active|Inactive") String status
) {
    public boolean isEmpty() {
        return firstName == null
            && lastName == null
            && dob == null
            && gender == null
            && photoUrl == null
            && contactNumber == null
            && specialization == null
            && bio == null
            && hireDate == null
            && status == null;
    }
}
