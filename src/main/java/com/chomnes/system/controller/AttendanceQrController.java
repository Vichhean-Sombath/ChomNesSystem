package com.chomnes.system.controller;

import com.chomnes.system.model.User;
import com.chomnes.system.service.AttendanceQrService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AttendanceQrController {
    private final AttendanceQrService attendanceQrService;

    public AttendanceQrController(AttendanceQrService attendanceQrService) {
        this.attendanceQrService = attendanceQrService;
    }

    @PostMapping({"/api/attendance-records/{scheduleId}/qr", "/attendance-record/qr/{scheduleId}"})
    public Map<String, Object> issue(
        @PathVariable Integer scheduleId,
        @AuthenticationPrincipal Jwt principal
    ) {
        return attendanceQrService.issue(scheduleId, requesterId(principal), requesterRole(principal));
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
        if (roles == null || roles.size() != 1) throw new AccessDeniedException("Invalid role in token.");
        try {
            return User.Role.valueOf(roles.get(0));
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid role in token.");
        }
    }
}
