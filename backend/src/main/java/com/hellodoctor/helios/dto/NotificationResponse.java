package com.hellodoctor.helios.dto;

import com.hellodoctor.helios.model.Notification;
import com.hellodoctor.helios.model.NotificationStatus;
import com.hellodoctor.helios.model.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        String subject,
        String content,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getSubject(),
                n.getContent(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getSentAt());
    }
}
