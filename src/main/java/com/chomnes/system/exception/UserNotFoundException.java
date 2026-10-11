package com.chomnes.system.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Integer userId) {
        super("User " + userId + " was not found.");
    }
}
