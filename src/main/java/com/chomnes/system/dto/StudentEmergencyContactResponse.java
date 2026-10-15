package com.chomnes.system.dto;

import com.chomnes.system.model.StudentEmergencyContact;

public record StudentEmergencyContactResponse(
        Integer contactId,
        Integer studentId,
        String contactName,
        String relationship,
        String phoneNumber,
        String email
) {
    public static StudentEmergencyContactResponse from(StudentEmergencyContact contact) {
        return new StudentEmergencyContactResponse(
                contact.getContactId(),
                contact.getStudentId(),
                contact.getContactName(),
                contact.getRelationship(),
                contact.getPhoneNumber(),
                contact.getEmail()
        );
    }
}
