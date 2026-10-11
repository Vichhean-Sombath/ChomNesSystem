package com.chomnes.system.dto;

import com.chomnes.system.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Integer userId,
        String username,
        String email,
        User.Role role,
        User.Status status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
