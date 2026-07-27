package com.hellodoctor.helios.config;

import com.hellodoctor.helios.model.Appointment;
import com.hellodoctor.helios.model.AppointmentStatus;
import com.hellodoctor.helios.model.Role;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.model.UserStatus;
import com.hellodoctor.helios.repository.AppointmentRepository;
import com.hellodoctor.helios.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN account on startup and, optionally, a set of sample doctors, patients, and
 * appointments for demos and local development. Intended for local/dev convenience; disable in
 * production via SEED_ADMIN_ENABLED=false (and SEED_SAMPLE_ENABLED=false).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Shared password for all seeded sample doctors. */
    private static final String SAMPLE_DOCTOR_PASSWORD = "Doctor@12345";

    /** Shared password for all seeded sample patients. */
    private static final String SAMPLE_PATIENT_PASSWORD = "Patient@12345";

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${helios.seed.admin.enabled:false}")
    private boolean seedEnabled;

    @Value("${helios.seed.admin.email:admin@hellodoctor.local}")
    private String adminEmail;

    @Value("${helios.seed.admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${helios.seed.sample.enabled:true}")
    private boolean seedSampleEnabled;

    public DataInitializer(
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        seedAdmin();
        if (seedSampleEnabled) {
            seedSampleData();
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .name("System Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(admin);
        log.info("Seeded default admin account: {}", adminEmail);
    }

    private void seedSampleData() {
        // Only seed once: skip if any doctor already exists.
        if (!userRepository.findByRole(Role.DOCTOR).isEmpty()) {
            return;
        }

        User drChen =
                saveDoctor("Dr. Sarah Chen", "sarah.chen@hellodoctor.local", "Cardiology", "+15550101001");
        User drPatel =
                saveDoctor("Dr. Raj Patel", "raj.patel@hellodoctor.local", "Dermatology", "+15550101002");
        User drTurner =
                saveDoctor("Dr. Emily Turner", "emily.turner@hellodoctor.local", "Pediatrics", "+15550101003");

        User john = savePatient("John Doe", "john.doe@example.com", "+15550202001");
        User maria = savePatient("Maria Garcia", "maria.garcia@example.com", "+15550202002");
        User liam = savePatient("Liam Smith", "liam.smith@example.com", "+15550202003");

        LocalDate today = LocalDate.now();
        List<Appointment> appointments = List.of(
                appointment(john, drChen, today.plusDays(1).atTime(LocalTime.of(10, 0)),
                        AppointmentStatus.PENDING, "Routine cardiac check-up."),
                appointment(john, drPatel, today.plusDays(3).atTime(LocalTime.of(14, 30)),
                        AppointmentStatus.APPROVED, "Follow-up on skin allergy."),
                appointment(maria, drTurner, today.plusDays(2).atTime(LocalTime.of(9, 0)),
                        AppointmentStatus.APPROVED, "Child vaccination consultation."),
                appointment(maria, drChen, today.minusDays(1).atTime(LocalTime.of(11, 0)),
                        AppointmentStatus.COMPLETED, "Reviewed ECG results."),
                appointment(liam, drPatel, today.plusDays(5).atTime(LocalTime.of(16, 0)),
                        AppointmentStatus.PENDING, "New mole examination."),
                appointment(liam, drTurner, today.minusDays(2).atTime(LocalTime.of(15, 0)),
                        AppointmentStatus.CANCELLED, "Rescheduled by patient."));
        appointmentRepository.saveAll(appointments);

        log.info(
                "Seeded sample data: 3 doctors, 3 patients, {} appointments.", appointments.size());
        log.info(
                "Sample logins -> doctors: <email> / {} | patients: <email> / {}",
                SAMPLE_DOCTOR_PASSWORD, SAMPLE_PATIENT_PASSWORD);
    }

    private User saveDoctor(String name, String email, String specialty, String phone) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(SAMPLE_DOCTOR_PASSWORD))
                .role(Role.DOCTOR)
                .status(UserStatus.ACTIVE)
                .specialty(specialty)
                .phone(phone)
                .build());
    }

    private User savePatient(String name, String email, String phone) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(SAMPLE_PATIENT_PASSWORD))
                .role(Role.PATIENT)
                .status(UserStatus.ACTIVE)
                .phone(phone)
                .build());
    }

    private Appointment appointment(
            User patient, User doctor, LocalDateTime when, AppointmentStatus status, String notes) {
        return Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(when)
                .status(status)
                .notes(notes)
                .build();
    }
}
