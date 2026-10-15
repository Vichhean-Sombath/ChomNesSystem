package com.chomnes.system.exception;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(Integer invoiceId) {
        super("Invoice with ID " + invoiceId + " was not found.");
    }
}
