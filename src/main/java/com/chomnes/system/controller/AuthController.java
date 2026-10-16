package com.chomnes.system.controller;

import com.chomnes.system.dto.ChangePasswordRequest;
import com.chomnes.system.dto.BootstrapAdminRequest;
import com.chomnes.system.dto.LoginRequest;
import com.chomnes.system.dto.ScholarisAuthResponse;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.dto.CreateUserRequest;
import com.chomnes.system.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/user"})
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/login"})
    public Object login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if ("/user/login".equals(servletRequest.getRequestURI())) {
            return authService.scholarisLogin(request);
        }
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<ScholarisAuthResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Logout successful!");
    }

    @PostMapping("/bootstrap-admin")
    public ResponseEntity<UserResponse> bootstrapAdmin(@Valid @RequestBody BootstrapAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.bootstrapAdmin(request));
    }

    @PutMapping({"/password", "/change-password"})
    public Object changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt principal,
            HttpServletRequest servletRequest
    ) {
        authService.changePassword(Integer.valueOf(principal.getSubject()), request);
        if ("/user/change-password".equals(servletRequest.getRequestURI())) {
            return Map.of("message", "Password changed successfully!");
        }
        return ResponseEntity.noContent().build();
    }
}
