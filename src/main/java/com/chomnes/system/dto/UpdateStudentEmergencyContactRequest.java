package com.chomnes.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateStudentEmergencyContactRequest(
    @Size(max = 100) String contactName,
    @Size(max = 50) String relationship,
    Boolean clearRelationship,
    @Size(max = 20) String phoneNumber,
    @Email @Size(max = 100) String email,
    Boolean clearEmail
) {
    public boolean isEmpty() {
        return contactName == null
            && relationship == null
            && clearRelationship == null
            && phoneNumber == null
            && email == null
            && clearEmail == null;
    }
}
