-- Provider retries and duplicate webhooks must not record the same charge twice.
CREATE UNIQUE INDEX uq_payments_transaction_reference
    ON payments(transaction_reference)
    WHERE transaction_reference IS NOT NULL;
