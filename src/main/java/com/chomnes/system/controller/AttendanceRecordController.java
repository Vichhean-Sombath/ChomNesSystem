package com.chomnes.system.controller;

import com.chomnes.system.dto.AttendanceRecordResponse;
import com.chomnes.system.dto.CreateAttendanceRecordRequest;
import com.chomnes.system.dto.UpdateAttendanceRecordRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.AttendanceRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/attendance-records", "/attendance-record"})
public class AttendanceRecordController {
    private final AttendanceRecordService attendanceService;

    public AttendanceRecordController(AttendanceRecordService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping({"", "/data"})
    public List<AttendanceRecordResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return attendanceService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{attendanceId}", "/data/{attendanceId}"})
    public AttendanceRecordResponse findById(
        @PathVariable Integer attendanceId,
        @AuthenticationPrincipal Jwt principal
    ) {
        return attendanceService.findById(attendanceId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<AttendanceRecordResponse> create(
        @Valid @RequestBody CreateAttendanceRecordRequest request,
        @AuthenticationPrincipal Jwt principal
    ) {
        AttendanceRecordResponse created = attendanceService.create(
            request,
            requesterId(principal),
            requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{attendanceId}")
            .buildAndExpand(created.attendanceId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{attendanceId}", "/update/{attendanceId}"})
    public AttendanceRecordResponse update(
        @PathVariable Integer attendanceId,
        @Valid @RequestBody UpdateAttendanceRecordRequest request,
        @AuthenticationPrincipal Jwt principal
    ) {
        return attendanceService.update(
            attendanceId,
            request,
            requesterId(principal),
            requesterRole(principal)
        );
    }

    @DeleteMapping({"/{attendanceId}", "/delete/{attendanceId}"})
    public Map<String, String> delete(@PathVariable Integer attendanceId) {
        attendanceService.delete(attendanceId);
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
