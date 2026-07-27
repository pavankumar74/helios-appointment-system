package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Used by doctors/admins to update an appointment. All fields optional except status handling
 * is validated in the service layer.
 */
public record AppointmentUpdateRequest(
        @NotNull AppointmentStatus status,
        LocalDateTime scheduledAt,
        @Size(max = 1000) String notes) {
}
