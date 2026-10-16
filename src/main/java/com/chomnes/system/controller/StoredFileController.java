package com.chomnes.system.controller;

import com.chomnes.system.model.User;
import com.chomnes.system.service.CertificateService;
import com.chomnes.system.service.FileStorageService;
import com.chomnes.system.service.LessonResourceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class StoredFileController {
    private final FileStorageService files;
    private final CertificateService certificates;
    private final LessonResourceService resources;

    public StoredFileController(
        FileStorageService files, CertificateService certificates, LessonResourceService resources
    ) {
        this.files = files;
        this.certificates = certificates;
        this.resources = resources;
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<FileSystemResource> download(
        @PathVariable String fileName,
        @AuthenticationPrincipal Jwt principal
    ) {
        String url = "/files/" + fileName;
        Integer userId = userId(principal);
        User.Role role = role(principal);
        boolean authorized = certificates.canReadFile(url, userId, role)
            || resources.canReadFile(url, userId, role);
        if (!authorized) throw new AccessDeniedException("You cannot access this file.");
        FileStorageService.StoredFileInfo stored = files.load(fileName);
        MediaType type = MediaType.parseMediaType(stored.contentType());
        return ResponseEntity.ok()
            .contentType(type)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(stored.fileName(), StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noCache())
            .body(new FileSystemResource(stored.path()));
    }

    private Integer userId(Jwt principal) {
        try {
            return Integer.valueOf(principal.getSubject());
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid user identity in token.");
        }
    }

    private User.Role role(Jwt principal) {
        List<String> roles = principal.getClaimAsStringList("roles");
        if (roles == null || roles.size() != 1) throw new AccessDeniedException("Invalid role in token.");
        try {
            return User.Role.valueOf(roles.getFirst());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid role in token.");
        }
    }
}
