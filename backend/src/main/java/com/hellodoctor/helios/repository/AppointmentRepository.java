package com.hellodoctor.helios.repository;

import com.hellodoctor.helios.model.Appointment;
import com.hellodoctor.helios.model.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("select a from Appointment a join fetch a.patient join fetch a.doctor "
            + "where a.patient.id = :patientId order by a.scheduledAt desc")
    List<Appointment> findByPatientIdOrderByScheduledAtDesc(@Param("patientId") Long patientId);

    @Query("select a from Appointment a join fetch a.patient join fetch a.doctor "
            + "where a.doctor.id = :doctorId order by a.scheduledAt desc")
    List<Appointment> findByDoctorIdOrderByScheduledAtDesc(@Param("doctorId") Long doctorId);

    @Query("select a from Appointment a join fetch a.patient join fetch a.doctor "
            + "order by a.scheduledAt desc")
    List<Appointment> findAllWithParties();

    @Query("select a from Appointment a join fetch a.patient join fetch a.doctor where a.id = :id")
    Optional<Appointment> findByIdWithParties(@Param("id") Long id);

    boolean existsByDoctorIdAndScheduledAtAndStatusIn(
            Long doctorId, LocalDateTime scheduledAt, List<AppointmentStatus> statuses);
}
