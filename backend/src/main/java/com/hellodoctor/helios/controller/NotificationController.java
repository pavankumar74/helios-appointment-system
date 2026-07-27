package com.hellodoctor.helios.controller;

import com.hellodoctor.helios.dto.NotificationRequest;
import com.hellodoctor.helios.dto.NotificationResponse;
import com.hellodoctor.helios.security.SecurityUser;
import com.hellodoctor.helios.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Send a notification asynchronously (doctors/admins). */
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse body = NotificationResponse.from(notificationService.createFromRequest(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    /** List the current user's notifications. */
    @GetMapping
    public List<NotificationResponse> mine(@AuthenticationPrincipal SecurityUser principal) {
        return notificationService.forUser(principal.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
