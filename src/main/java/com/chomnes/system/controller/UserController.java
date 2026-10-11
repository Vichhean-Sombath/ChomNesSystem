package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateUserRequest;
import com.chomnes.system.dto.UpdateUserRequest;
import com.chomnes.system.dto.UserResponse;
import com.chomnes.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/users")
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
    public UserResponse findById(@PathVariable("userId") Integer userId) {
        return userService.findById(userId);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.userId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{userId}")
    public UserResponse update(
            @PathVariable("userId") Integer userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(userId, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivate(@PathVariable("userId") Integer userId) {
        userService.deactivate(userId);
        return ResponseEntity.noContent().build();
    }
}
