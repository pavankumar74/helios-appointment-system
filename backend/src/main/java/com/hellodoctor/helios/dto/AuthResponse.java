package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long userId,
        String name,
        String email,
        Role role) {
}
