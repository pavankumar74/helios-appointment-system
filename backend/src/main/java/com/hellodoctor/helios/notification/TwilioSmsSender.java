package com.hellodoctor.helios.notification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Real SMS delivery via the Twilio REST API. Activated with
 * {@code helios.notifications.sms.provider=twilio} and valid credentials. No Twilio SDK dependency
 * is required — this calls the HTTP API directly.
 */
@Component
@ConditionalOnProperty(name = "helios.notifications.sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsSender(
            @Value("${helios.notifications.sms.twilio.account-sid}") String accountSid,
            @Value("${helios.notifications.sms.twilio.auth-token}") String authToken,
            @Value("${helios.notifications.sms.twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01")
                .build();
    }

    @Override
    public void send(String toPhoneNumber, String body) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("Recipient phone number is required for SMS.");
        }
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toPhoneNumber);
        form.add("From", fromNumber);
        form.add("Body", body);

        restClient.post()
                .uri("/Accounts/{sid}/Messages.json", accountSid)
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();

        log.debug("SMS sent to {} via Twilio", toPhoneNumber);
    }
}
