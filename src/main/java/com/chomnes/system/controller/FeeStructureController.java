package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateFeeStructureRequest;
import com.chomnes.system.dto.FeeStructureResponse;
import com.chomnes.system.dto.UpdateFeeStructureRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.FeeStructureService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fee-structures")
public class FeeStructureController {
    private final FeeStructureService feeStructureService;

    public FeeStructureController(FeeStructureService feeStructureService) {
        this.feeStructureService = feeStructureService;
    }

    @GetMapping
    public List<FeeStructureResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return feeStructureService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/{feeId}")
    public FeeStructureResponse findById(@PathVariable Integer feeId, @AuthenticationPrincipal Jwt principal) {
        return feeStructureService.findById(feeId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<FeeStructureResponse> create(@Valid @RequestBody CreateFeeStructureRequest request) {
        FeeStructureResponse created = feeStructureService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{feeId}")
                .buildAndExpand(created.feeId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{feeId}")
    public FeeStructureResponse update(
            @PathVariable Integer feeId,
            @Valid @RequestBody UpdateFeeStructureRequest request
    ) {
        return feeStructureService.update(feeId, request);
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
