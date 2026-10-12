package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateStudentRequest;
import com.chomnes.system.dto.StudentResponse;
import com.chomnes.system.dto.UpdateStudentRequest;
import com.chomnes.system.model.User;
import com.chomnes.system.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentResponse> findAll() {
        return studentService.findAll();
    }

    @GetMapping("/{studentId}")
    public StudentResponse findById(
            @PathVariable("studentId") Integer studentId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return studentService.findById(studentId, requesterId(principal), requesterRole(principal));
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody CreateStudentRequest request) {
        StudentResponse created = studentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{studentId}")
                .buildAndExpand(created.studentId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{studentId}")
    public StudentResponse update(
            @PathVariable("studentId") Integer studentId,
            @Valid @RequestBody UpdateStudentRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return studentService.update(
                studentId,
                request,
                requesterId(principal),
                requesterRole(principal)
        );
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deactivate(@PathVariable("studentId") Integer studentId) {
        studentService.deactivate(studentId);
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
