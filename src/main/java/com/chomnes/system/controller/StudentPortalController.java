package com.chomnes.system.controller;

import com.chomnes.system.dto.StudentAttendanceCheckInRequest;
import com.chomnes.system.service.StudentPortalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/student/me")
public class StudentPortalController {
    private final StudentPortalService portalService;

    public StudentPortalController(StudentPortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@AuthenticationPrincipal Jwt principal) {
        return portalService.summary(userId(principal));
    }

    @GetMapping("/classes")
    public Object classes(@AuthenticationPrincipal Jwt principal) {
        return portalService.classes(userId(principal));
    }

    @GetMapping("/schedule")
    public Object schedule(@AuthenticationPrincipal Jwt principal) {
        return portalService.schedule(userId(principal));
    }

    @GetMapping("/grades")
    public Object grades(@AuthenticationPrincipal Jwt principal) {
        return portalService.grades(userId(principal));
    }

    @GetMapping("/attendance")
    public Object attendance(@AuthenticationPrincipal Jwt principal) {
        return portalService.attendance(userId(principal));
    }

    @PostMapping("/attendance/checkin")
    public ResponseEntity<Map<String, Object>> checkIn(
            @Valid @RequestBody StudentAttendanceCheckInRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.checkIn(userId(principal), request));
    }

    @GetMapping("/invoices")
    public Object invoices(@AuthenticationPrincipal Jwt principal) {
        return portalService.invoices(userId(principal));
    }

    @GetMapping("/certificates")
    public Object certificates(@AuthenticationPrincipal Jwt principal) {
        return portalService.certificates(userId(principal));
    }

    @GetMapping("/resources")
    public Object resources(@AuthenticationPrincipal Jwt principal) {
        return portalService.resources(userId(principal));
    }

    @GetMapping("/profile")
    public Object profile(@AuthenticationPrincipal Jwt principal) {
        return portalService.profile(userId(principal));
    }

    private Integer userId(Jwt principal) {
        try {
            return Integer.valueOf(principal.getSubject());
        } catch (NumberFormatException exception) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid user identity in token.");
        }
    }
}
