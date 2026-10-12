package com.chomnes.system.exception;

public class SubjectNotFoundException extends RuntimeException {
    public SubjectNotFoundException(Integer subjectId) {
        super("Subject " + subjectId + " was not found.");
    }
}
