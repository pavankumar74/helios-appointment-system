package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        Role role,
        @Size(max = 120) String specialty,
        @Size(max = 20) String phone) {
}
