package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateLessonResourceRequest;
import com.chomnes.system.dto.LessonResourceResponse;
import com.chomnes.system.dto.UpdateLessonResourceRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.LessonResourceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/lesson-resources")
public class LessonResourceController {
    private final LessonResourceService resourceService;

    public LessonResourceController(LessonResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<LessonResourceResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return resourceService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping("/{resourceId}")
    public LessonResourceResponse findById(
            @PathVariable Integer resourceId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return resourceService.findById(resourceId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<LessonResourceResponse> create(
            @Valid @RequestBody CreateLessonResourceRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        LessonResourceResponse created = resourceService.create(
                request, requesterId(principal), requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{resourceId}")
                .buildAndExpand(created.resourceId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{resourceId}")
    public LessonResourceResponse update(
            @PathVariable Integer resourceId,
            @Valid @RequestBody UpdateLessonResourceRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return resourceService.update(resourceId, request, requesterId(principal), requesterRole(principal));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer resourceId,
            @AuthenticationPrincipal Jwt principal
    ) {
        resourceService.delete(resourceId, requesterId(principal), requesterRole(principal));
        return ResponseEntity.noContent().build();
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
