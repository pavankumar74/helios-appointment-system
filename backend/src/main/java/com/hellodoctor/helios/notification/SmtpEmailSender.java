package com.hellodoctor.helios.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real SMTP email sender backed by Spring's {@link JavaMailSender}. When
 * {@code helios.notifications.mail.enabled=false} (default for local dev), messages are logged
 * instead of sent so no SMTP server is required.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;

    @Value("${helios.notifications.mail.enabled:false}")
    private boolean enabled;

    @Value("${helios.notifications.mail.from:no-reply@hellodoctor.local}")
    private String from;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("[DEV] Email -> {} | {} - {}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.debug("Email sent to {}", to);
    }
}
