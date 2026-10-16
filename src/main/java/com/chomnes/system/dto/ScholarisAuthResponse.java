package com.chomnes.system.dto;

import com.chomnes.system.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ScholarisAuthResponse(
    String message,
    String token,
    ScholarisUser user
) {
    public record ScholarisUser(
        @JsonProperty("user_id") Integer userId,
        String username,
        String email,
        User.Role role,
        User.Status status,
        @JsonProperty("teacher_id") Integer teacherId,
        @JsonProperty("student_id") Integer studentId
    ) {
    }
}
