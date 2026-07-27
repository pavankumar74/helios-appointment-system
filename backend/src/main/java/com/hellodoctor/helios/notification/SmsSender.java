package com.hellodoctor.helios.notification;

/**
 * Sends an SMS. Implementations must throw on failure so the caller can apply retry logic.
 */
public interface SmsSender {

    void send(String toPhoneNumber, String body) throws Exception;
}
