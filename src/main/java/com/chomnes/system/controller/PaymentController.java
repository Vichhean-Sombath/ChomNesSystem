package com.chomnes.system.controller;

import com.chomnes.system.dto.CreatePaymentRequest;
import com.chomnes.system.dto.PaymentResponse;
import com.chomnes.system.dto.UpdatePaymentRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/payments", "/payment"})
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping({"", "/data"})
    public List<PaymentResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return paymentService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{paymentId}", "/data/{paymentId}"})
    public PaymentResponse findById(@PathVariable Integer paymentId, @AuthenticationPrincipal Jwt principal) {
        return paymentService.findById(paymentId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<PaymentResponse> recordPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        PaymentResponse created = paymentService.recordPayment(
                request, requesterId(principal), requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(created.paymentId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{paymentId}", "/update/{paymentId}"})
    public PaymentResponse update(
            @PathVariable Integer paymentId,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        if (requesterRole(principal) != User.Role.Admin) {
            throw new AccessDeniedException("Only an administrator can update a payment.");
        }
        return paymentService.update(paymentId, request);
    }

    @DeleteMapping({"/{paymentId}", "/delete/{paymentId}"})
    public Map<String, String> delete(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal Jwt principal
    ) {
        if (requesterRole(principal) != User.Role.Admin) {
            throw new AccessDeniedException("Only an administrator can delete a payment.");
        }
        paymentService.delete(paymentId);
        return Map.of("message", "deleted");
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
        if (roles == null || roles.size() != 1) {
            throw new AccessDeniedException("Invalid role in token.");
        }
        try {
            return User.Role.valueOf(roles.get(0));
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid role in token.");
        }
    }
}
