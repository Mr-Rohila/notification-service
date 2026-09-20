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
                    "verificationUrl": "http://localhost:9000/auth/api/v1/auth/register/verify?token=token-value",
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
                .isEqualTo("http://localhost:9000/auth/api/v1/auth/register/verify?token=token-value");
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
                    "resetUrl": "http://localhost:9000/auth/password-reset?token=reset-token",
                    "tokenExpiresAt": "2026-08-29T08:30:00Z"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        PasswordResetRequestedPayload payload =
                objectMapper.convertValue(event.payload(), PasswordResetRequestedPayload.class);

        assertThat(event.eventType()).isEqualTo("PASSWORD_RESET_REQUESTED");
        assertThat(payload.resetUrl()).isEqualTo("http://localhost:9000/auth/password-reset?token=reset-token");
    }

    @Test
    void deserializesAccountActivatedEvent() throws Exception {
        String json =
                """
                {
                  "eventId": "9e1f8891-9647-62f0-166d-g29he3h12cg9",
                  "eventType": "ACCOUNT_ACTIVATED",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-13T08:00:00Z",
                  "source": "auth-server",
                  "serviceRequestId": "req-welcome-1",
                  "payload": {
                    "userId": "0194a2b3-c4d5-7890-abcd-ef1234567890",
                    "displayName": "Jane Doe",
                    "email": "user@example.com",
                    "loginUrl": "http://localhost:9000/auth/login"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        AccountActivatedPayload payload = objectMapper.convertValue(event.payload(), AccountActivatedPayload.class);

        assertThat(event.eventType()).isEqualTo("ACCOUNT_ACTIVATED");
        assertThat(payload.loginUrl()).isEqualTo("http://localhost:9000/auth/login");
        assertThat(payload.email()).isEqualTo("user@example.com");
    }

    @Test
    void deserializesPaymentSubmittedEventFromUserServiceFixture() throws Exception {
        String json =
                """
                {
                  "eventId": "11111111-1111-1111-1111-111111111111",
                  "eventType": "PAYMENT_SUBMITTED",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-05T10:30:00Z",
                  "source": "user-service",
                  "serviceRequestId": "req-1",
                  "payload": {
                    "userId": "0194a2b3-c4d5-7890-abcd-ef1234567890",
                    "displayName": "Jane Doe",
                    "email": "user@example.com",
                    "transactionId": "txn-1",
                    "amount": "100.0000",
                    "currency": "INR",
                    "status": "PENDING",
                    "type": "CREDIT"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        TransactionNotificationPayload payload =
                objectMapper.convertValue(event.payload(), TransactionNotificationPayload.class);

        assertThat(event.eventType()).isEqualTo("PAYMENT_SUBMITTED");
        assertThat(payload.amount()).isEqualTo("100.0000");
        assertThat(payload.transactionId()).isEqualTo("txn-1");
        assertThat(payload.currency()).isEqualTo("INR");
    }

    @Test
    void deserializesSubscriptionPurchasedEventFromTaskServiceFixture() throws Exception {
        String json =
                """
                {
                  "eventId": "22222222-2222-2222-2222-222222222222",
                  "eventType": "SUBSCRIPTION_PURCHASED",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-06T15:00:00Z",
                  "source": "task-service",
                  "serviceRequestId": "req-1",
                  "payload": {
                    "userId": "0194a2b3-c4d5-7890-abcd-ef1234567890",
                    "displayName": "Jane Doe",
                    "email": "user@example.com",
                    "purchaseId": "purchase-1",
                    "planTitle": "Starter",
                    "channelTitle": "My Channel",
                    "amount": "199.0000",
                    "currency": "INR",
                    "status": "ACTIVE"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        CampaignNotificationPayload payload =
                objectMapper.convertValue(event.payload(), CampaignNotificationPayload.class);

        assertThat(event.eventType()).isEqualTo("SUBSCRIPTION_PURCHASED");
        assertThat(payload.purchaseId()).isEqualTo("purchase-1");
        assertThat(payload.planTitle()).isEqualTo("Starter");
        assertThat(payload.channelTitle()).isEqualTo("My Channel");
        assertThat(payload.amount()).isEqualTo("199.0000");
    }

    @Test
    void deserializesAdminWalletCreditedEvent() throws Exception {
        String json =
                """
                {
                  "eventId": "33333333-3333-3333-3333-333333333333",
                  "eventType": "ADMIN_WALLET_CREDITED",
                  "eventVersion": 1,
                  "occurredAt": "2026-09-20T08:15:00Z",
                  "source": "user-service",
                  "serviceRequestId": "req-admin-credit-1",
                  "payload": {
                    "userId": "0194a2b3-c4d5-7890-abcd-ef1234567890",
                    "displayName": "Jane Doe",
                    "email": "user@example.com",
                    "transactionId": "txn-admin-1",
                    "amount": "500.0000",
                    "currency": "INR",
                    "status": "APPROVED",
                    "type": "CREDIT"
                  }
                }
                """;

        NotificationEvent event = objectMapper.readValue(json, NotificationEvent.class);
        TransactionNotificationPayload payload =
                objectMapper.convertValue(event.payload(), TransactionNotificationPayload.class);

        assertThat(event.eventType()).isEqualTo("ADMIN_WALLET_CREDITED");
        assertThat(NotificationEventType.isKnown("ADMIN_WALLET_CREDITED")).isTrue();
        assertThat(payload.amount()).isEqualTo("500.0000");
        assertThat(payload.transactionId()).isEqualTo("txn-admin-1");
        assertThat(payload.status()).isEqualTo("APPROVED");
        assertThat(payload.type()).isEqualTo("CREDIT");
    }
}
