package com.chomnes.system.exception;

public class FinalGradeNotFoundException extends RuntimeException {
    public FinalGradeNotFoundException(Integer finalGradeId) {
        super("Final grade with ID " + finalGradeId + " was not found.");
    }
}
