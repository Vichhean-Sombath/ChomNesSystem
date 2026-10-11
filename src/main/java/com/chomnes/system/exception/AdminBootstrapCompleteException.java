package com.chomnes.system.exception;

public class AdminBootstrapCompleteException extends RuntimeException {
    public AdminBootstrapCompleteException() {
        super("An administrator account has already been configured.");
    }
}
