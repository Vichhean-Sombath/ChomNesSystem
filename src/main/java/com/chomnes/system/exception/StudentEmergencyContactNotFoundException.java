package com.chomnes.system.exception;

public class StudentEmergencyContactNotFoundException extends RuntimeException {
    public StudentEmergencyContactNotFoundException(Integer contactId) {
        super("Student emergency contact with ID " + contactId + " was not found.");
    }
}
