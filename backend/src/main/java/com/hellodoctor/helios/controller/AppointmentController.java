package com.hellodoctor.helios.controller;

import com.hellodoctor.helios.dto.AppointmentRequest;
import com.hellodoctor.helios.dto.AppointmentResponse;
import com.hellodoctor.helios.dto.AppointmentUpdateRequest;
import com.hellodoctor.helios.security.SecurityUser;
import com.hellodoctor.helios.service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** List appointments visible to the current user (role-based). */
    @GetMapping
    public List<AppointmentResponse> list(@AuthenticationPrincipal SecurityUser principal) {
        return appointmentService.listForCurrentUser(principal).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(
            @AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        return AppointmentResponse.from(appointmentService.getVisible(principal, id));
    }

    /** Book an appointment (patient only). */
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse body = AppointmentResponse.from(appointmentService.book(principal, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Update / approve / reject an appointment (doctor or admin). */
    @PutMapping("/{id}")
    public AppointmentResponse update(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentUpdateRequest request) {
        return AppointmentResponse.from(appointmentService.update(principal, id, request));
    }

    /** Cancel an appointment (owning patient or admin). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal SecurityUser principal, @PathVariable Long id) {
        appointmentService.cancel(principal, id);
        return ResponseEntity.noContent().build();
    }
}
