package com.hellodoctor.helios.service;

import com.hellodoctor.helios.dto.NotificationRequest;
import com.hellodoctor.helios.model.Notification;
import com.hellodoctor.helios.model.NotificationStatus;
import com.hellodoctor.helios.model.NotificationType;
import com.hellodoctor.helios.model.User;
import com.hellodoctor.helios.repository.NotificationRepository;
import com.hellodoctor.helios.repository.UserRepository;
import com.hellodoctor.helios.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Creates notification records and hands off delivery to the async {@link NotificationDispatcher}.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher dispatcher;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            NotificationDispatcher dispatcher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.dispatcher = dispatcher;
    }

    @Transactional
    public Notification createFromRequest(NotificationRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return enqueue(user, request.type(), request.subject(), request.content());
    }

    /**
     * Persist a PENDING notification then trigger asynchronous delivery.
     */
    @Transactional
    public Notification enqueue(User user, NotificationType type, String subject, String content) {
        String recipient = type == NotificationType.SMS ? user.getPhone() : user.getEmail();
        Notification notification = Notification.builder()
                .userId(user.getId())
                .recipient(recipient == null ? "" : recipient)
                .type(type)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.PENDING)
                .build();
        notification = notificationRepository.save(notification);
        dispatchAfterCommit(notification.getId());
        return notification;
    }

    /**
     * Trigger async delivery only after the current transaction commits, so the background thread
     * is guaranteed to see the persisted notification. If no transaction is active, dispatch now.
     */
    private void dispatchAfterCommit(Long notificationId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.dispatch(notificationId);
                }
            });
        } else {
            dispatcher.dispatch(notificationId);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> forUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
