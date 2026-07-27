package com.hellodoctor.helios.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull Long doctorId,
        @NotNull @Future LocalDateTime scheduledAt,
        @Size(max = 1000) String notes) {
}
