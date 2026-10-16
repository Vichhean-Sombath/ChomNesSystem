package com.chomnes.system.exception;

public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(Integer certificateId) {
        super("Certificate with ID " + certificateId + " was not found.");
    }

    public CertificateNotFoundException(String message) {
        super(message);
    }
}
