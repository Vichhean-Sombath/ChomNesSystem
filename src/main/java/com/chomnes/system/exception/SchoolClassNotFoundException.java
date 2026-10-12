package com.chomnes.system.exception;

public class SchoolClassNotFoundException extends RuntimeException {
    public SchoolClassNotFoundException(Integer classId) {
        super("Class " + classId + " was not found.");
    }
}
