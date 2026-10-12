package com.chomnes.system.exception;

public class TeacherNotFoundException extends RuntimeException {
    public TeacherNotFoundException(Integer teacherId) {
        super("Teacher " + teacherId + " was not found.");
    }
}
