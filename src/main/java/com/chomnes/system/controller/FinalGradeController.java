package com.chomnes.system.controller;

import com.chomnes.system.dto.ComputeFinalGradeRequest;
import com.chomnes.system.dto.CreateFinalGradeRequest;
import com.chomnes.system.dto.FinalGradeResponse;
import com.chomnes.system.dto.UpdateFinalGradeRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.FinalGradeService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/final-grades", "/final-grade"})
public class FinalGradeController {
    private final FinalGradeService finalGradeService;

    public FinalGradeController(FinalGradeService finalGradeService) {
        this.finalGradeService = finalGradeService;
    }

    @GetMapping({"", "/data"})
    public List<FinalGradeResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return finalGradeService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{finalGradeId}", "/data/{finalGradeId}"})
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

    @PostMapping("/create")
    public FinalGradeResponse create(
            @Valid @RequestBody CreateFinalGradeRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return finalGradeService.create(request, requesterId(principal), requesterRole(principal));
    }

    @PutMapping({"/{finalGradeId}", "/update/{finalGradeId}"})
    public FinalGradeResponse update(
            @PathVariable Integer finalGradeId,
            @Valid @RequestBody UpdateFinalGradeRequest request
    ) {
        return finalGradeService.update(finalGradeId, request);
    }

    @DeleteMapping({"/{finalGradeId}", "/delete/{finalGradeId}"})
    public Map<String, String> delete(@PathVariable Integer finalGradeId) {
        finalGradeService.delete(finalGradeId);
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
