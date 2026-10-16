package com.chomnes.system.controller;

import com.chomnes.system.dto.BakongPaymentRequest;
import com.chomnes.system.dto.PaymentResponse;
import com.chomnes.system.dto.StripeCheckoutRequest;
import com.chomnes.system.dto.StripeVerifyRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.PaymentIntegrationService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentIntegrationController {
    private final PaymentIntegrationService integrations;

    public PaymentIntegrationController(PaymentIntegrationService integrations) {
        this.integrations = integrations;
    }

    @GetMapping("/stripe/config")
    public Map<String, Object> stripeConfig() {
        return integrations.stripeConfig();
    }

    @PostMapping("/stripe/checkout")
    public Map<String, Object> stripeCheckout(
            @Valid @RequestBody StripeCheckoutRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return integrations.createStripeCheckout(request.invoiceId(), request.successUrl(), request.cancelUrl(),
                requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/stripe/success")
    public Map<String, Object> stripeSuccess(@RequestParam(name = "session_id") String sessionId) {
        return integrations.stripeSuccess(sessionId);
    }

    @GetMapping("/stripe/cancel")
    public Map<String, String> stripeCancel() {
        return Map.of("message", "Payment cancelled by user.");
    }

    @PostMapping("/stripe/verify")
    public PaymentResponse stripeVerify(
            @Valid @RequestBody StripeVerifyRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return integrations.verifyStripeCheckout(request.sessionId(), requesterId(principal), requesterRole(principal));
    }

    /** This compatibility route requires a real Stripe session; it never trusts a client-supplied invoice as proof. */
    @PostMapping("/stripe/record")
    public PaymentResponse stripeRecord(
            @Valid @RequestBody StripeVerifyRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return integrations.verifyStripeCheckout(request.sessionId(), requesterId(principal), requesterRole(principal));
    }

    @PostMapping("/stripe/webhook")
    public Map<String, Boolean> stripeWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        integrations.handleStripeWebhook(rawBody, signature);
        return Map.of("received", true);
    }

    @PostMapping("/bakong/qr")
    public Map<String, Object> bakongQr(
            @Valid @RequestBody InvoiceIdRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return integrations.generateBakongQr(request.invoiceId(), requesterId(principal), requesterRole(principal));
    }

    @PostMapping("/bakong/verify")
    public Map<String, Object> bakongVerify(
            @Valid @RequestBody BakongPaymentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return integrations.verifyBakong(request.md5(), requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/bakong/account")
    public Map<String, Object> bakongAccount() {
        return integrations.checkBakongAccount();
    }

    @PostMapping("/bakong/webhook")
    public Map<String, Object> bakongWebhook(@RequestBody com.fasterxml.jackson.databind.JsonNode payload) {
        return integrations.handleBakongWebhook(payload);
    }

    private Integer requesterId(Jwt principal) {
        try {
            return Integer.valueOf(principal.getSubject());
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid user identity in token.");
        }
    }

    private User.Role requesterRole(Jwt principal) {
        List<String> roles = principal.getClaimAsStringList("roles");
        if (roles == null || roles.size() != 1) throw new AccessDeniedException("Invalid role in token.");
        try {
            return User.Role.valueOf(roles.getFirst());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid role in token.");
        }
    }

    public record InvoiceIdRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Integer invoiceId) { }
}
