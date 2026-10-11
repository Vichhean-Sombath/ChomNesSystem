package com.chomnes.system.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
