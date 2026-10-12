package com.chomnes.system.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Integer studentId) {
        super("Student " + studentId + " was not found.");
    }
}
