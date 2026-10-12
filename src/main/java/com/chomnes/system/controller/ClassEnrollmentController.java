package com.chomnes.system.controller;

import com.chomnes.system.dto.ClassEnrollmentResponse;
import com.chomnes.system.dto.CreateClassEnrollmentRequest;
import com.chomnes.system.dto.UpdateClassEnrollmentRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.ClassEnrollmentService;
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
@RequestMapping("/api/class-enrollments")
public class ClassEnrollmentController {
    private final ClassEnrollmentService enrollmentService;

    public ClassEnrollmentController(ClassEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<ClassEnrollmentResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return enrollmentService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/{enrollmentId}")
    public ClassEnrollmentResponse findById(
            @PathVariable Integer enrollmentId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return enrollmentService.findById(enrollmentId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<ClassEnrollmentResponse> create(@Valid @RequestBody CreateClassEnrollmentRequest request) {
        ClassEnrollmentResponse created = enrollmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{enrollmentId}")
                .buildAndExpand(created.enrollmentId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{enrollmentId}")
    public ClassEnrollmentResponse update(
            @PathVariable Integer enrollmentId,
            @Valid @RequestBody UpdateClassEnrollmentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return enrollmentService.update(
                enrollmentId,
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
