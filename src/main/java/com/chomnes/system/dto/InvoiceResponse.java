package com.chomnes.system.dto;

import com.chomnes.system.model.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Integer invoiceId,
        String invoiceNumber,
        Integer studentId,
        Integer feeId,
        Integer semesterId,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        String status,
        LocalDateTime createdAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceNumber(),
                invoice.getStudentId(),
                invoice.getFeeId(),
                invoice.getSemesterId(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getTotalAmount(),
                invoice.getAmountPaid(),
                invoice.getStatus(),
                invoice.getCreatedAt()
        );
    }
}
