package com.chomnes.system.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String field) {
        super("A user with this " + field + " already exists.");
    }
}
