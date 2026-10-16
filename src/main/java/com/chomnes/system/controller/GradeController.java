package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateGradeRequest;
import com.chomnes.system.dto.GradeResponse;
import com.chomnes.system.dto.UpdateGradeRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.GradeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/grades", "/grade"})
public class GradeController {
    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping({"", "/data"})
    public List<GradeResponse> findAll(
        @RequestParam(name = "assessment_id", required = false) Integer assessmentId,
        @AuthenticationPrincipal Jwt principal
    ) {
        return gradeService.findAll(requesterId(principal), requesterRole(principal), assessmentId);
    }

    @GetMapping({"/{gradeId}", "/data/{gradeId}"})
    public GradeResponse findById(@PathVariable Integer gradeId, @AuthenticationPrincipal Jwt principal) {
        return gradeService.findById(gradeId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<GradeResponse> create(
        @Valid @RequestBody CreateGradeRequest request,
        @AuthenticationPrincipal Jwt principal
    ) {
        GradeResponse created = gradeService.create(request, requesterId(principal), requesterRole(principal));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{gradeId}")
            .buildAndExpand(created.gradeId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{gradeId}", "/update/{gradeId}"})
    public GradeResponse update(
        @PathVariable Integer gradeId,
        @Valid @RequestBody UpdateGradeRequest request,
        @AuthenticationPrincipal Jwt principal
    ) {
        return gradeService.update(gradeId, request, requesterId(principal), requesterRole(principal));
    }

    @DeleteMapping({"/{gradeId}", "/delete/{gradeId}"})
    public Map<String, String> delete(@PathVariable Integer gradeId) {
        gradeService.delete(gradeId);
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
