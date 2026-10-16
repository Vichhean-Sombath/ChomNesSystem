package com.chomnes.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class StripeProviderClient {
    private static final String API_URL = "https://api.stripe.com/v1";
    private static final long SIGNATURE_TOLERANCE_SECONDS = 300;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String secretKey;
    private final String publicKey;
    private final String webhookSecret;
    private final String serverUrl;

    public StripeProviderClient(
            ObjectMapper objectMapper,
            @Value("${app.payments.stripe.secret-key:}") String secretKey,
            @Value("${app.payments.stripe.public-key:}") String publicKey,
            @Value("${app.payments.stripe.webhook-secret:}") String webhookSecret,
            @Value("${app.payments.server-url:}") String serverUrl
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = sanitize(secretKey);
        this.publicKey = sanitize(publicKey);
        this.webhookSecret = sanitize(webhookSecret);
        this.serverUrl = sanitize(serverUrl);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String publicKey() {
        if (publicKey.isBlank()) throw new IllegalStateException("Stripe public key not configured.");
        return publicKey;
    }

    public StripeSession createCheckoutSession(
            Integer invoiceId,
            Integer studentId,
            String invoiceNumber,
            String feeName,
            long amountCents,
            String successUrl,
            String cancelUrl
    ) {
        requireSecretKey();
        String baseUrl = normalizedServerUrl();
        validateRedirect(successUrl);
        validateRedirect(cancelUrl);
        String success = successUrl == null || successUrl.isBlank()
                ? baseUrl + "/payment/stripe/success?session_id={CHECKOUT_SESSION_ID}" : successUrl;
        String cancel = cancelUrl == null || cancelUrl.isBlank()
                ? baseUrl + "/payment/stripe/cancel" : cancelUrl;

        Map<String, String> form = Map.ofEntries(
                Map.entry("mode", "payment"),
                Map.entry("payment_method_types[0]", "card"),
                Map.entry("line_items[0][price_data][currency]", "usd"),
                Map.entry("line_items[0][price_data][unit_amount]", Long.toString(amountCents)),
                Map.entry("line_items[0][price_data][product_data][name]", blankFallback(feeName, "School fee")),
                Map.entry("line_items[0][price_data][product_data][description]", "Invoice " + invoiceNumber),
                Map.entry("line_items[0][quantity]", "1"),
                Map.entry("metadata[invoice_id]", invoiceId.toString()),
                Map.entry("metadata[student_id]", studentId.toString()),
                Map.entry("metadata[invoice_number]", invoiceNumber),
                Map.entry("success_url", success),
                Map.entry("cancel_url", cancel)
        );
        return parseSession(send("POST", "/checkout/sessions", form));
    }

    public StripeSession retrieveCheckoutSession(String sessionId) {
        requireSecretKey();
        if (sessionId == null || !sessionId.matches("cs_[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("A valid Stripe checkout session ID is required.");
        }
        return parseSession(send("GET", "/checkout/sessions/" + sessionId, Map.of()));
    }

    public JsonNode verifyAndParseWebhook(byte[] payload, String signatureHeader) {
        if (webhookSecret.isBlank()) throw new IllegalStateException("Stripe webhook secret not configured.");
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Stripe webhook signature is missing.");
        }
        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            if ("t".equals(pair[0])) timestamp = pair[1];
            else if ("v1".equals(pair[0])) signatures.add(pair[1]);
        }
        if (timestamp == null || signatures.isEmpty()) throw new IllegalArgumentException("Stripe webhook signature is invalid.");
        long epoch;
        try {
            epoch = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Stripe webhook timestamp is invalid.");
        }
        if (Math.abs(Instant.now().getEpochSecond() - epoch) > SIGNATURE_TOLERANCE_SECONDS) {
            throw new IllegalArgumentException("Stripe webhook timestamp is outside the allowed tolerance.");
        }
        byte[] signedPayload = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
        byte[] raw = new byte[signedPayload.length + payload.length];
        System.arraycopy(signedPayload, 0, raw, 0, signedPayload.length);
        System.arraycopy(payload, 0, raw, signedPayload.length, payload.length);
        byte[] expected = hmacSha256(webhookSecret, raw);
        boolean valid = signatures.stream().anyMatch(signature -> {
            try {
                return MessageDigest.isEqual(expected, hexBytes(signature));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        });
        if (!valid) throw new IllegalArgumentException("Stripe webhook signature verification failed.");
        try {
            return objectMapper.readTree(payload);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Stripe webhook body is not valid JSON.");
        }
    }

    private JsonNode send(String method, String path, Map<String, String> form) {
        String credentials = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(API_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Basic " + credentials)
                .header("Accept", "application/json");
        if ("POST".equals(method)) {
            String body = form.entrySet().stream()
                    .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                    .collect(java.util.stream.Collectors.joining("&"));
            request.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            request.GET();
        }
        try {
            HttpResponse<byte[]> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = json.path("error").path("message").asText("Stripe request failed.");
                throw new IllegalStateException(message);
            }
            return json;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Stripe request was interrupted.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Stripe service could not be reached.", exception);
        }
    }

    private StripeSession parseSession(JsonNode json) {
        JsonNode metadata = json.path("metadata");
        return new StripeSession(json.path("id").asText(null), json.path("url").asText(null),
                json.path("payment_status").asText("unpaid"),
                json.path("amount_total").isNumber() ? json.path("amount_total").longValue() : null,
                json.path("currency").asText(null), metadata.path("invoice_id").asText(null),
                metadata.path("student_id").asText(null));
    }

    private void requireSecretKey() {
        if (secretKey.isBlank()) throw new IllegalStateException("Stripe is not configured.");
        if (!secretKey.startsWith("sk_")) {
            throw new IllegalStateException("STRIPE_SECRET_KEY must be a Stripe secret key (sk_...).");
        }
    }

    private String normalizedServerUrl() {
        if (serverUrl.isBlank()) throw new IllegalStateException("SERVER_URL is not configured.");
        validateRedirect(serverUrl);
        return serverUrl.replaceAll("/+$", "");
    }

    private void validateRedirect(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.replace("{CHECKOUT_SESSION_ID}", "checkout-session-id"));
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Stripe redirect URLs must be absolute HTTP(S) URLs.");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Stripe redirect URLs must be absolute HTTP(S) URLs.");
        }
    }

    private static byte[] hmacSha256(String secret, byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify Stripe webhook signature.", exception);
        }
    }

    private static byte[] hexBytes(String value) {
        if (value.length() != 64 || !value.matches("[A-Fa-f0-9]{64}")) {
            throw new IllegalArgumentException("Invalid signature digest.");
        }
        return java.util.HexFormat.of().parseHex(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record StripeSession(
            String id,
            String url,
            String paymentStatus,
            Long amountTotal,
            String currency,
            String invoiceId,
            String studentId
    ) { }
}
