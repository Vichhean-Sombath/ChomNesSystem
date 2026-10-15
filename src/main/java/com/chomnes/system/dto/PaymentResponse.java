package com.chomnes.system.dto;

import com.chomnes.system.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Integer paymentId,
        Integer invoiceId,
        LocalDate paymentDate,
        BigDecimal amount,
        String paymentMethod,
        String receiptUrl,
        String transactionReference,
        Integer recordedBy,
        String notes
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getInvoiceId(),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getReceiptUrl(),
                payment.getTransactionReference(),
                payment.getRecordedBy(),
                payment.getNotes()
        );
    }
}
