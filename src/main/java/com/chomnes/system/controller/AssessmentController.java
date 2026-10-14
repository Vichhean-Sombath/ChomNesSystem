package com.chomnes.system.controller;

import com.chomnes.system.dto.AssessmentResponse;
import com.chomnes.system.dto.CreateAssessmentRequest;
import com.chomnes.system.dto.UpdateAssessmentRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.AssessmentService;
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
@RequestMapping("/api/assessments")
public class AssessmentController {
    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<AssessmentResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return assessmentService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/{assessmentId}")
    public AssessmentResponse findById(
            @PathVariable Integer assessmentId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return assessmentService.findById(assessmentId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> create(
            @Valid @RequestBody CreateAssessmentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        AssessmentResponse created = assessmentService.create(
                request,
                requesterId(principal),
                requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{assessmentId}")
                .buildAndExpand(created.assessmentId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{assessmentId}")
    public AssessmentResponse update(
            @PathVariable Integer assessmentId,
            @Valid @RequestBody UpdateAssessmentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return assessmentService.update(
                assessmentId,
                request,
                requesterId(principal),
                requesterRole(principal)
        );
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
