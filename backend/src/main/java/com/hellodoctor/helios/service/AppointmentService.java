package com.hellodoctor.helios.service;

import com.hellodoctor.helios.dto.AppointmentRequest;
import com.hellodoctor.helios.dto.AppointmentUpdateRequest;
import com.hellodoctor.helios.exception.BadRequestException;
import com.hellodoctor.helios.exception.ConflictException;
import com.hellodoctor.helios.exception.ForbiddenActionException;
import com.hellodoctor.helios.exception.ResourceNotFoundException;
import com.hellodoctor.helios.model.Appointment;
import com.hellodoctor.helios.model.AppointmentStatus;
import com.hellodoctor.helios.model.NotificationType;
import com.hellodoctor.helios.model.Role;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.repository.AppointmentRepository;
import com.hellodoctor.helios.repository.UserRepository;
import com.hellodoctor.helios.security.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.APPROVED);

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Appointment book(SecurityUser principal, AppointmentRequest request) {
        if (principal.getRole() != Role.PATIENT) {
            throw new ForbiddenActionException("Only patients can book appointments.");
        }
        if (request.scheduledAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Appointment time must be in the future.");
        }

        User patient = getUser(principal.getId());
        User doctor = userRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        if (doctor.getRole() != Role.DOCTOR) {
            throw new BadRequestException("Selected user is not a doctor.");
        }

        boolean slotTaken = appointmentRepository.existsByDoctorIdAndScheduledAtAndStatusIn(
                doctor.getId(), request.scheduledAt(), ACTIVE_STATUSES);
        if (slotTaken) {
            throw new ConflictException("The selected time slot is no longer available.");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(request.scheduledAt())
                .status(AppointmentStatus.PENDING)
                .notes(request.notes())
                .build();
        appointment = appointmentRepository.save(appointment);

        notificationService.enqueue(doctor, NotificationType.EMAIL,
                "New appointment request",
                "You have a new appointment request from " + patient.getName()
                        + " for " + appointment.getScheduledAt() + ".");
        notificationService.enqueue(patient, NotificationType.EMAIL,
                "Appointment requested",
                "Your appointment with Dr. " + doctor.getName()
                        + " for " + appointment.getScheduledAt() + " is pending confirmation.");

        return appointment;
    }

    @Transactional(readOnly = true)
    public List<Appointment> listForCurrentUser(SecurityUser principal) {
        return switch (principal.getRole()) {
            case PATIENT -> appointmentRepository.findByPatientIdOrderByScheduledAtDesc(principal.getId());
            case DOCTOR -> appointmentRepository.findByDoctorIdOrderByScheduledAtDesc(principal.getId());
            case ADMIN -> appointmentRepository.findAllWithParties();
        };
    }

    @Transactional(readOnly = true)
    public Appointment getVisible(SecurityUser principal, Long id) {
        Appointment appointment = getAppointment(id);
        assertCanView(principal, appointment);
        return appointment;
    }

    @Transactional
    public Appointment update(SecurityUser principal, Long id, AppointmentUpdateRequest request) {
        Appointment appointment = getAppointment(id);

        if (principal.getRole() == Role.PATIENT) {
            throw new ForbiddenActionException("Patients cannot update appointments; use cancel instead.");
        }
        if (principal.getRole() == Role.DOCTOR
                && !appointment.getDoctor().getId().equals(principal.getId())) {
            throw new ForbiddenActionException("You can only manage your own appointments.");
        }

        AppointmentStatus previous = appointment.getStatus();
        appointment.setStatus(request.status());
        if (request.scheduledAt() != null) {
            if (request.scheduledAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Appointment time must be in the future.");
            }
            appointment.setScheduledAt(request.scheduledAt());
        }
        if (request.notes() != null) {
            appointment.setNotes(request.notes());
        }
        appointment = appointmentRepository.save(appointment);

        if (previous != request.status()) {
            notifyStatusChange(appointment);
        }
        return appointment;
    }

    @Transactional
    public void cancel(SecurityUser principal, Long id) {
        Appointment appointment = getAppointment(id);

        boolean isOwningPatient = principal.getRole() == Role.PATIENT
                && appointment.getPatient().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        if (!isOwningPatient && !isAdmin) {
            throw new ForbiddenActionException("Only the booking patient or an admin can cancel this appointment.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException("This appointment can no longer be cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        notifyStatusChange(appointment);
    }

    private void notifyStatusChange(Appointment appointment) {
        String subject = "Appointment " + appointment.getStatus().name().toLowerCase();
        String when = String.valueOf(appointment.getScheduledAt());
        notificationService.enqueue(appointment.getPatient(), NotificationType.EMAIL, subject,
                "Your appointment with Dr. " + appointment.getDoctor().getName()
                        + " on " + when + " is now " + appointment.getStatus().name() + ".");
        notificationService.enqueue(appointment.getDoctor(), NotificationType.EMAIL, subject,
                "Appointment with " + appointment.getPatient().getName()
                        + " on " + when + " is now " + appointment.getStatus().name() + ".");
    }

    private void assertCanView(SecurityUser principal, Appointment appointment) {
        switch (principal.getRole()) {
            case ADMIN -> {
                /* full access */
            }
            case DOCTOR -> {
                if (!appointment.getDoctor().getId().equals(principal.getId())) {
                    throw new ForbiddenActionException("You can only view your own appointments.");
                }
            }
            case PATIENT -> {
                if (!appointment.getPatient().getId().equals(principal.getId())) {
                    throw new ForbiddenActionException("You can only view your own appointments.");
                }
            }
        }
    }

    private Appointment getAppointment(Long id) {
        return appointmentRepository.findByIdWithParties(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
