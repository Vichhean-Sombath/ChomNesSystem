package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateUserRequest;
import com.chomnes.system.dto.UpdateUserRequest;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/users", "/user"})
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{userId}")
    public UserResponse findById(
            @PathVariable("userId") Integer userId,
            @AuthenticationPrincipal Jwt principal
    ) {
        return userService.findById(userId, requesterId(principal), isAdministrator(principal));
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest servletRequest
    ) {
        UserResponse created = userService.create(request, servletRequest.getRequestURI().startsWith("/user/"));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.userId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{userId}", "/update/{userId}"})
    public UserResponse update(
            @PathVariable("userId") Integer userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        return userService.update(userId, request, requesterId(principal), isAdministrator(principal));
    }

    @DeleteMapping({"/{userId}", "/disable/{userId}"})
    public Object deactivate(
            @PathVariable("userId") Integer userId,
            @AuthenticationPrincipal Jwt principal,
            HttpServletRequest servletRequest
    ) {
        UserResponse deactivated = userService.deactivate(userId, requesterId(principal));
        if (servletRequest.getRequestURI().startsWith("/user/disable/")) {
            return deactivated;
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/enable/{userId}")
    public UserResponse activate(@PathVariable Integer userId) {
        return userService.activate(userId);
    }

    private Integer requesterId(Jwt principal) {
        try {
            return Integer.valueOf(principal.getSubject());
        } catch (NumberFormatException exception) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid user identity in token.");
        }
    }

    private boolean isAdministrator(Jwt principal) {
        List<String> roles = principal.getClaimAsStringList("roles");
        return roles != null && roles.contains("Admin");
    }
}
