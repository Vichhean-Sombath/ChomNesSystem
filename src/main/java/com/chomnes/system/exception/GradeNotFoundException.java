package com.chomnes.system.exception;

public class GradeNotFoundException extends RuntimeException {
    public GradeNotFoundException(Integer gradeId) {
        super("Grade with ID " + gradeId + " was not found.");
    }
}
