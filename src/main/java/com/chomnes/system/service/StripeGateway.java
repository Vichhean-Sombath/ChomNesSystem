package com.chomnes.system.service;

/** Application-facing contract for the Stripe payment provider. */
public interface StripeGateway {
    String publicKey();

    StripeSession createCheckoutSession(
        Integer invoiceId,
        Integer studentId,
        String invoiceNumber,
        String feeName,
        long amountCents,
        String successUrl,
        String cancelUrl
    );

    StripeSession retrieveCheckoutSession(String sessionId);

    StripeWebhookEvent verifyWebhook(byte[] payload, String signatureHeader);

    record StripeSession(
        String id,
        String url,
        String paymentStatus,
        Long amountTotal,
        String currency,
        String invoiceId,
        String studentId
    ) { }

    record StripeWebhookEvent(String type, String sessionId) { }
}
