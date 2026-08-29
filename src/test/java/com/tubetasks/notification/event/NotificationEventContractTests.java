package com.tubetasks.notification.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationEventContractTests {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void deserializesVerificationEventFromAuthServerFixture() throws Exception {
        String json =
                """
                {
                  "eventId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                  "eventType": "EMAIL_VERIFICATION_REQUESTED",
                  "eventVersion": 1,
                  "occurredAt": "2026-08-29T08:00:00Z",
                  "source": "auth-server",
                  "serviceRequestId": "req-abc-123",
                  "payload": {
                    "userId": "0194a2b3-c4d5-7890-abcd-ef1234567890",
                    "displayName": "Jane Doe",
                    "email": "user@example.com",
                    "verificationToken": "token-value",
                    "verificationUrl": "http://localhost:9000/api/v1/auth/register/verify?token=token-value",
                    "tokenExpiresAt": "2026-08-30T08:00:00Z",
                    "from": "ignored@example.com",
                    "template": "ignored-template"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        EmailVerificationRequestedPayload payload =
                objectMapper.convertValue(event.payload(), EmailVerificationRequestedPayload.class);

        assertThat(event.eventType()).isEqualTo("EMAIL_VERIFICATION_REQUESTED");
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(payload.email()).isEqualTo("user@example.com");
        assertThat(payload.verificationUrl())
                .isEqualTo("http://localhost:9000/api/v1/auth/register/verify?token=token-value");
        assertThat(payload.tokenExpiresAt()).isEqualTo(Instant.parse("2026-08-30T08:00:00Z"));
    }

    @Test
    void deserializesPasswordResetEventFromAuthServerFixture() throws Exception {
        String json =
                """
                {
                  "eventId": "8d0f7780-8536-51ef-055c-f18gd2g01bf8",
                  "eventType": "PASSWORD_RESET_REQUESTED",
                  "eventVersion": 1,
                  "occurredAt": "2026-08-29T08:00:00Z",
                  "source": "auth-server",
                  "serviceRequestId": "req-reset-1",
                  "payload": {
                    "userId": "0194a2b3-c4d5-7890-abcd-ef1234567890",
                    "displayName": "Jane Doe",
                    "email": "user@example.com",
                    "resetToken": "reset-token",
                    "resetUrl": "http://localhost:9000/password-reset?token=reset-token",
                    "tokenExpiresAt": "2026-08-29T08:30:00Z"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        PasswordResetRequestedPayload payload =
                objectMapper.convertValue(event.payload(), PasswordResetRequestedPayload.class);

        assertThat(event.eventType()).isEqualTo("PASSWORD_RESET_REQUESTED");
        assertThat(payload.resetUrl()).isEqualTo("http://localhost:9000/password-reset?token=reset-token");
    }
}
