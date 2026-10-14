package com.chomnes.system.exception;

public class AssessmentNotFoundException extends RuntimeException {
    public AssessmentNotFoundException(Integer assessmentId) {
        super("Assessment " + assessmentId + " was not found.");
    }
}
