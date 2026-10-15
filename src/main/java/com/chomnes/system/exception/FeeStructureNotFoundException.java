package com.chomnes.system.exception;

public class FeeStructureNotFoundException extends RuntimeException {
    public FeeStructureNotFoundException(Integer feeId) {
        super("Fee structure with ID " + feeId + " was not found.");
    }
}
