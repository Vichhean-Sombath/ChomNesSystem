package com.chomnes.system.exception;

public class BakongQrRequestNotFoundException extends RuntimeException {
    public BakongQrRequestNotFoundException(Integer qrId) {
        super("Bakong QR request with ID " + qrId + " was not found.");
    }

    public BakongQrRequestNotFoundException(String md5) {
        super("Bakong QR request with MD5 " + md5 + " was not found.");
    }
}
