package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationType type,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 2000) String content) {
}
