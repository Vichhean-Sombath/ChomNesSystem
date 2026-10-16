package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateStudentEmergencyContactRequest;
import com.chomnes.system.dto.StudentEmergencyContactResponse;
import com.chomnes.system.dto.UpdateStudentEmergencyContactRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.StudentEmergencyContactService;
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
@RequestMapping({"/api/student-emergency-contacts", "/student-emergency-contact"})
public class StudentEmergencyContactController {
    private final StudentEmergencyContactService contactService;

    public StudentEmergencyContactController(StudentEmergencyContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping({"", "/data"})
    public List<StudentEmergencyContactResponse> findAll(@AuthenticationPrincipal Jwt principal) {
        return contactService.findAll(requesterId(principal), requesterRole(principal));
    }

    @GetMapping({"/{contactId}", "/data/{contactId}"})
    public StudentEmergencyContactResponse findById(
            @PathVariable Integer contactId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return contactService.findById(contactId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<StudentEmergencyContactResponse> create(
            @Valid @RequestBody CreateStudentEmergencyContactRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        StudentEmergencyContactResponse created = contactService.create(
                request, requesterId(principal), requesterRole(principal)
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{contactId}")
                .buildAndExpand(created.contactId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{contactId}", "/update/{contactId}"})
    public StudentEmergencyContactResponse update(
            @PathVariable Integer contactId,
            @Valid @RequestBody UpdateStudentEmergencyContactRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return contactService.update(contactId, request, requesterId(principal), requesterRole(principal));
    }

    @DeleteMapping({"/{contactId}", "/delete/{contactId}"})
    public ResponseEntity<Void> delete(
            @PathVariable Integer contactId,
            @AuthenticationPrincipal Jwt principal
    ) {
        contactService.delete(contactId, requesterId(principal), requesterRole(principal));
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
