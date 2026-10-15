package com.chomnes.system.exception;

public class LessonResourceNotFoundException extends RuntimeException {
    public LessonResourceNotFoundException(Integer resourceId) {
        super("Lesson resource with ID " + resourceId + " was not found.");
    }
}
