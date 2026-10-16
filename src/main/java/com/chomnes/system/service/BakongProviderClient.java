package com.chomnes.system.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class BakongProviderClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String accountId;
    private final String merchantName;
    private final String merchantCity;
    private final String merchantPhone;
    private final String storeLabel;
    private final String apiUrl;
    private final String apiToken;
    private final boolean apiEnabled;
    private final BigDecimal khrPerUsd;

    public BakongProviderClient(
        ObjectMapper objectMapper,
        @Value("${app.payments.bakong.account-id:}") String accountId,
        @Value("${app.payments.bakong.merchant-name:}") String merchantName,
        @Value("${app.payments.bakong.merchant-city:}") String merchantCity,
        @Value("${app.payments.bakong.merchant-phone:}") String merchantPhone,
        @Value("${app.payments.bakong.store-label:}") String storeLabel,
        @Value("${app.payments.bakong.api-url:https://api-bakong.nbc.gov.kh}") String apiUrl,
        @Value("${app.payments.bakong.api-token:}") String apiToken,
        @Value("${app.payments.bakong.api-enabled:false}") boolean apiEnabled,
        @Value("${app.payments.bakong.khr-per-usd:4000}") BigDecimal khrPerUsd
    ) {
        this.objectMapper = objectMapper;
        this.accountId = sanitize(accountId);
        this.merchantName = sanitize(merchantName);
        this.merchantCity = sanitize(merchantCity);
        this.merchantPhone = sanitize(merchantPhone);
        this.storeLabel = sanitize(storeLabel);
        this.apiUrl = normalizeApiUrl(apiUrl);
        this.apiToken = sanitize(apiToken);
        this.apiEnabled = apiEnabled;
        this.khrPerUsd = khrPerUsd == null || khrPerUsd.signum() <= 0 ? BigDecimal.valueOf(4000) : khrPerUsd;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public GeneratedKhqr generate(BigDecimal amountUsd, String invoiceNumber) {
        requireMerchantConfiguration();
        if (amountUsd == null || amountUsd.signum() <= 0) {
            throw new IllegalArgumentException("Invoice balance must be greater than zero to generate a KHQR.");
        }
        BigDecimal amountKhr = amountUsd.multiply(khrPerUsd).setScale(0, java.math.RoundingMode.HALF_UP);
        long expiryMillis = System.currentTimeMillis() + Duration.ofMinutes(30).toMillis();
        IndividualInfo info = new IndividualInfo();
        info.setBakongAccountId(accountId);
        info.setCurrency(KHQRCurrency.KHR);
        info.setAmount(amountKhr.doubleValue());
        info.setMerchantName(truncate(merchantName, 25));
        info.setMerchantCity(truncate(merchantCity.isBlank() ? "Phnom Penh" : merchantCity, 15));
        info.setBillNumber(truncate(invoiceNumber, 25));
        info.setMobileNumber(truncate(merchantPhone, 25));
        info.setStoreLabel(truncate(storeLabel.isBlank() ? merchantName : storeLabel, 25));
        info.setMerchantCategoryCode("8299");
        info.setExpirationTimestamp(expiryMillis);

        KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(info);
        if (response == null || response.getKHQRStatus() == null || response.getKHQRStatus().getCode() != 0
            || response.getData() == null || response.getData().getQr() == null) {
            String message = response == null || response.getKHQRStatus() == null
                ? "KHQR generation failed." : response.getKHQRStatus().getMessage();
            throw new IllegalStateException(message == null || message.isBlank() ? "KHQR generation failed." : message);
        }
        return new GeneratedKhqr(response.getData().getQr(), response.getData().getMd5(), amountKhr,
            amountUsd.setScale(2, java.math.RoundingMode.HALF_UP), expiryMillis, true);
    }

    public Map<String, Object> checkAccount() {
        requireMerchantConfiguration();
        if (!apiEnabled || apiToken.isBlank()) return Map.of("enabled", false);
        return Map.of("enabled", true, "result", post("/v1/check_bakong_account", Map.of("accountId", accountId)));
    }

    public JsonNode checkTransactionByMd5(String md5) {
        if (!apiEnabled || apiToken.isBlank()) {
            throw new IllegalStateException("Bakong API is not enabled or not configured.");
        }
        return post("/v1/check_transaction_by_md5", Map.of("md5", md5));
    }

    public BigDecimal khrPerUsd() {
        return khrPerUsd;
    }

    public String accountId() {
        return accountId;
    }

    private JsonNode post(String path, Map<String, String> bodyFields) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(bodyFields);
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(json.path("responseMessage").asString("Bakong API request failed."));
            }
            return json;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bakong API request was interrupted.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Bakong API request could not be completed.", exception);
        }
    }

    private void requireMerchantConfiguration() {
        if (accountId.isBlank() || merchantName.isBlank() || merchantCity.isBlank() || merchantPhone.isBlank()) {
            throw new IllegalStateException("Bakong merchant details are not configured.");
        }
    }

    private static String normalizeApiUrl(String value) {
        String result = sanitize(value);
        if (result.endsWith("/v1")) result = result.substring(0, result.length() - 3);
        return result.replaceAll("/+$", "");
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private static String truncate(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }

    public record GeneratedKhqr(
        String qrString,
        String md5,
        BigDecimal amountKhr,
        BigDecimal amountUsd,
        long expirationTimestamp,
        boolean verified
    ) {
    }
}
