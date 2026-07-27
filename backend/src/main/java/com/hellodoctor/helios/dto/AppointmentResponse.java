package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.Appointment;
import com.hellodoctor.helios.model.AppointmentStatus;
import java.time.Instant;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        LocalDateTime scheduledAt,
        AppointmentStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPatient().getName(),
                a.getDoctor().getId(),
                a.getDoctor().getName(),
                a.getScheduledAt(),
                a.getStatus(),
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
