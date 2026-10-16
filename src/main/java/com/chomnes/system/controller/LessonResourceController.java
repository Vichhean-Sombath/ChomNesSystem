package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateLessonResourceRequest;
import com.chomnes.system.dto.LessonResourceResponse;
import com.chomnes.system.dto.UpdateLessonResourceRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.LessonResourceService;
import com.chomnes.system.service.FileStorageService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/lesson-resources", "/lesson_resource"})
public class LessonResourceController {
    private final LessonResourceService resourceService;
    private final FileStorageService files;

    public LessonResourceController(LessonResourceService resourceService, FileStorageService files) {
        this.resourceService = resourceService;
        this.files = files;
    }

    @GetMapping({"", "/data"})
    public List<LessonResourceResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return resourceService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{resourceId}", "/data/{resourceId}"})
    public LessonResourceResponse findById(
        @PathVariable Integer resourceId,
        @AuthenticationPrincipal Jwt principal
    ) {
        return resourceService.findById(resourceId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
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

    @PostMapping({"/{resourceId}/file", "/upload/{resourceId}"})
    public Map<String, Object> uploadFile(
        @PathVariable Integer resourceId,
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal Jwt principal
    ) {
        FileStorageService.StoredFile stored = files.store(file);
        try {
            resourceService.attachFile(resourceId, stored.url(), requesterId(principal), requesterRole(principal));
        } catch (RuntimeException exception) {
            files.discard(stored);
            throw exception;
        }
        return Map.of("fileUrl", stored.url(), "fileName", stored.originalName(), "size", stored.size());
    }

    @PutMapping({"/{resourceId}", "/update/{resourceId}"})
    public LessonResourceResponse update(
        @PathVariable Integer resourceId,
        @Valid @RequestBody UpdateLessonResourceRequest request,
        @AuthenticationPrincipal Jwt principal
    ) {
        return resourceService.update(resourceId, request, requesterId(principal), requesterRole(principal));
    }

    @DeleteMapping({"/{resourceId}", "/delete/{resourceId}"})
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
