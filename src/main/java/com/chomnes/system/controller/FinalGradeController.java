package com.chomnes.system.controller;

import com.chomnes.system.dto.ComputeFinalGradeRequest;
import com.chomnes.system.dto.FinalGradeResponse;
import com.chomnes.system.model.User;
import com.chomnes.system.service.FinalGradeService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/final-grades")
public class FinalGradeController {
    private final FinalGradeService finalGradeService;

    public FinalGradeController(FinalGradeService finalGradeService) {
        this.finalGradeService = finalGradeService;
    }

    @GetMapping
    public List<FinalGradeResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return finalGradeService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/{finalGradeId}")
    public FinalGradeResponse findById(
            @PathVariable Integer finalGradeId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return finalGradeService.findById(finalGradeId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping("/compute")
    public FinalGradeResponse compute(
            @Valid @RequestBody ComputeFinalGradeRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return finalGradeService.compute(request, requesterId(principal), requesterRole(principal));
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
