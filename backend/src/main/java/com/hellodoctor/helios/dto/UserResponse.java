package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.Role;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.model.UserStatus;
import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        String specialty,
        String phone,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getSpecialty(),
                user.getPhone(),
                user.getCreatedAt());
    }
}
