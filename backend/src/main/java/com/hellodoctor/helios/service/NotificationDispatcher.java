package com.hellodoctor.helios.service;

import com.hellodoctor.helios.model.Notification;
import com.hellodoctor.helios.model.NotificationStatus;
import com.hellodoctor.helios.notification.EmailSender;
import com.hellodoctor.helios.notification.SmsSender;
import com.hellodoctor.helios.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Performs the actual (blocking) delivery of a notification on a background thread, with simple
 * retry logic. Persisted status is updated to SENT or FAILED (PRD section 9).
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public NotificationDispatcher(
            NotificationRepository notificationRepository,
            EmailSender emailSender,
            SmsSender smsSender) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    @Async("notificationExecutor")
    public void dispatch(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return;
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                deliver(notification);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(java.time.Instant.now());
                notification.setAttempts(attempt);
                notification.setLastError(null);
                notificationRepository.save(notification);
                return;
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Notification {} delivery attempt {}/{} failed: {}",
                        notificationId, attempt, MAX_ATTEMPTS, ex.getMessage());
            }
        }

        // All attempts exhausted -> mark FAILED (dead-letter candidate).
        notification.setStatus(NotificationStatus.FAILED);
        notification.setAttempts(MAX_ATTEMPTS);
        notification.setLastError(lastException == null ? "unknown" : lastException.getMessage());
        notificationRepository.save(notification);
        log.error("Notification {} permanently failed after {} attempts", notificationId, MAX_ATTEMPTS);
    }

    private void deliver(Notification notification) throws Exception {
        switch (notification.getType()) {
            case EMAIL -> emailSender.send(
                    notification.getRecipient(), notification.getSubject(), notification.getContent());
            case SMS -> smsSender.send(notification.getRecipient(), notification.getContent());
        }
    }
}
