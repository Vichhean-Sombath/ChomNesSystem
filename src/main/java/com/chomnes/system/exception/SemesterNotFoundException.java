package com.chomnes.system.exception;

public class SemesterNotFoundException extends RuntimeException {
    public SemesterNotFoundException(Integer semesterId) {
        super("Semester " + semesterId + " was not found.");
    }
}
