package com.hellodoctor.helios.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default SMS sender that logs messages instead of sending them. Active unless a real provider
 * (e.g. Twilio) is configured via {@code helios.notifications.sms.provider}.
 */
@Component
@ConditionalOnProperty(name = "helios.notifications.sms.provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String toPhoneNumber, String body) {
        log.info("[DEV] SMS -> {} | {}", toPhoneNumber, body);
    }
}
