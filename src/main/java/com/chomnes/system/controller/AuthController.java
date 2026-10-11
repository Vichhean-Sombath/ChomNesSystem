package com.chomnes.system.controller;

import com.chomnes.system.dto.ChangePasswordRequest;
import com.chomnes.system.dto.BootstrapAdminRequest;
import com.chomnes.system.dto.LoginRequest;
import com.chomnes.system.dto.LoginResponse;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<UserResponse> bootstrapAdmin(@Valid @RequestBody BootstrapAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.bootstrapAdmin(request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        authService.changePassword(Integer.valueOf(principal.getSubject()), request);
        return ResponseEntity.noContent().build();
    }
}
