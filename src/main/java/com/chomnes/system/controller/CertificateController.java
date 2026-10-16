package com.chomnes.system.controller;

import com.chomnes.system.dto.CertificateResponse;
import com.chomnes.system.dto.CreateCertificateRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.CertificateService;
import com.chomnes.system.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/certificates", "/certificate"})
public class CertificateController {
    private final CertificateService certificateService;
    private final FileStorageService files;

    public CertificateController(CertificateService certificateService, FileStorageService files) {
        this.certificateService = certificateService;
        this.files = files;
    }

    @GetMapping({"", "/data"})
    public List<CertificateResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return certificateService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{certificateId}", "/data/{certificateId}"})
    public CertificateResponse findById(
            @PathVariable Integer certificateId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return certificateService.findById(certificateId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<CertificateResponse> issue(
            @Valid @RequestBody CreateCertificateRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        CertificateResponse created = certificateService.issue(
                request, requesterId(principal), requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{certificateId}")
                .buildAndExpand(created.certificateId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping({"/{certificateId}/file", "/upload/{certificateId}"})
    public Map<String, Object> uploadFile(
            @PathVariable Integer certificateId,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) {
        FileStorageService.StoredFile stored = files.store(file);
        try {
            certificateService.attachFile(certificateId, stored.url());
        } catch (RuntimeException exception) {
            files.discard(stored);
            throw exception;
        }
        return Map.of("fileUrl", stored.url(), "fileName", stored.originalName(), "size", stored.size());
    }

    @DeleteMapping({"/{certificateId}", "/delete/{certificateId}"})
    public Map<String, String> delete(@PathVariable Integer certificateId) {
        certificateService.delete(certificateId);
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
