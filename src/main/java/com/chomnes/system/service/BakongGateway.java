package com.chomnes.system.service;

import java.math.BigDecimal;
import java.util.Map;

/** Application-facing contract for KHQR generation and Bakong transaction lookup. */
public interface BakongGateway {
    GeneratedKhqr generate(BigDecimal amountUsd, String invoiceNumber);

    Map<String, Object> checkAccount();

    TransactionCheck checkTransactionByMd5(String md5);

    BigDecimal khrPerUsd();

    String accountId();

    record GeneratedKhqr(
        String qrString,
        String md5,
        BigDecimal amountKhr,
        BigDecimal amountUsd,
        long expirationTimestamp,
        boolean verified
    ) { }

    record TransactionCheck(
        int responseCode,
        BigDecimal amount,
        String currency,
        String toAccountId,
        String hash,
        String receiptUrl
    ) { }
}
