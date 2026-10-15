package com.chomnes.system.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Integer paymentId) {
        super("Payment with ID " + paymentId + " was not found.");
    }
}
