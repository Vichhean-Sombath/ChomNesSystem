package com.chomnes.system.exception;

public class BakongQrRequestNotFoundException extends RuntimeException {
    public BakongQrRequestNotFoundException(Integer qrId) {
        super("Bakong QR request with ID " + qrId + " was not found.");
    }
}
