package com.chomnes.system.controller;

import com.chomnes.system.dto.BakongQrRequestResponse;
import com.chomnes.system.dto.CreateBakongQrRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.BakongQrRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bakong-qr-requests")
public class BakongQrRequestController {
    private final BakongQrRequestService requestService;

    public BakongQrRequestController(BakongQrRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    public List<BakongQrRequestResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return requestService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/{qrId}")
    public BakongQrRequestResponse findById(@PathVariable Integer qrId, @AuthenticationPrincipal Jwt principal) {
        return requestService.findById(qrId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<BakongQrRequestResponse> create(
            @Valid @RequestBody CreateBakongQrRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        BakongQrRequestResponse created = requestService.create(
                request, requesterId(principal), requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{qrId}")
                .buildAndExpand(created.qrId())
                .toUri();
        return ResponseEntity.created(location).body(created);
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
