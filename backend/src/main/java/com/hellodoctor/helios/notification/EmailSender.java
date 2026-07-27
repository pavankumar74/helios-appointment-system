package com.hellodoctor.helios.notification;

/**
 * Sends an email. Implementations must throw on failure so the caller can apply retry logic.
 */
public interface EmailSender {

    void send(String to, String subject, String body) throws Exception;
}
