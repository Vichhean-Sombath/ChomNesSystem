package com.chomnes.system.exception;

public class ClassEnrollmentNotFoundException extends RuntimeException {
    public ClassEnrollmentNotFoundException(Integer enrollmentId) {
        super("Class enrollment " + enrollmentId + " was not found.");
    }
}
