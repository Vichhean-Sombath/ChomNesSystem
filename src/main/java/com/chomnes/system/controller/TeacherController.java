package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateTeacherRequest;
import com.chomnes.system.dto.TeacherResponse;
import com.chomnes.system.dto.UpdateTeacherRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping({"/api/teachers", "/teacher"})
public class TeacherController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping({"", "/data"})
    public List<TeacherResponse> findAll() {
        return teacherService.findAll();
    }

    @GetMapping({"/{teacherId}", "/data/{teacherId}"})
    public TeacherResponse findById(
        @PathVariable("teacherId") Integer teacherId,
        @AuthenticationPrincipal Jwt principal
    ) {
        return teacherService.findById(teacherId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody CreateTeacherRequest request) {
        TeacherResponse created = teacherService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{teacherId}")
            .buildAndExpand(created.teacherId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{teacherId}", "/update/{teacherId}"})
    public TeacherResponse update(
        @PathVariable("teacherId") Integer teacherId,
        @Valid @RequestBody UpdateTeacherRequest request,
        @AuthenticationPrincipal Jwt principal
    ) {
        return teacherService.update(
            teacherId,
            request,
            requesterId(principal),
            requesterRole(principal)
        );
    }

    @DeleteMapping("/{teacherId}")
    public ResponseEntity<Void> deactivate(@PathVariable("teacherId") Integer teacherId) {
        teacherService.deactivate(teacherId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
