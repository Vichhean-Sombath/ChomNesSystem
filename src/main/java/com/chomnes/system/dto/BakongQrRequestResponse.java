package com.chomnes.system.dto;

import com.chomnes.system.model.BakongQrRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BakongQrRequestResponse(
        Integer qrId,
        Integer invoiceId,
        String md5,
        BigDecimal amountKhr,
        BigDecimal amountUsd,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static BakongQrRequestResponse from(BakongQrRequest request) {
        return new BakongQrRequestResponse(
                request.getQrId(),
                request.getInvoiceId(),
                request.getMd5(),
                request.getAmountKhr(),
                request.getAmountUsd(),
                request.getStatus(),
                request.getExpiresAt(),
                request.getPaidAt(),
                request.getCreatedAt()
        );
    }
}
