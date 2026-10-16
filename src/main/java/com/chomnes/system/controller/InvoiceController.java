package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateInvoiceRequest;
import com.chomnes.system.dto.InvoiceResponse;
import com.chomnes.system.dto.UpdateInvoiceRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/invoices", "/invoice"})
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping({"", "/data"})
    public List<InvoiceResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return invoiceService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{invoiceId}", "/data/{invoiceId}"})
    public InvoiceResponse findById(@PathVariable Integer invoiceId, @AuthenticationPrincipal Jwt principal) {
        return invoiceService.findById(invoiceId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody CreateInvoiceRequest request,
            HttpServletRequest servletRequest
    ) {
        InvoiceResponse created = invoiceService.create(request, servletRequest.getRequestURI().startsWith("/invoice/"));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{invoiceId}")
                .buildAndExpand(created.invoiceId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{invoiceId}", "/update/{invoiceId}"})
    public InvoiceResponse update(
            @PathVariable Integer invoiceId,
            @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        return invoiceService.update(invoiceId, request);
    }

    @DeleteMapping({"/{invoiceId}", "/delete/{invoiceId}"})
    public Map<String, String> delete(@PathVariable Integer invoiceId) {
        invoiceService.delete(invoiceId);
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
