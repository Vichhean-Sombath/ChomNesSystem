package com.chomnes.system.dto;

import com.chomnes.system.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 50) String username,
        @Email @Size(max = 100) String email,
        @Size(min = 8, max = 72) String password,
        User.Role role,
        User.Status status
) {
    public boolean isEmpty() {
        return username == null && email == null && password == null && role == null && status == null;
    }
}
