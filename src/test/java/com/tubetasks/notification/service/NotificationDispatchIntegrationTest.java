package com.tubetasks.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.tubetasks.notification.config.TestJwtDecoderConfig;
import com.tubetasks.notification.event.AccountActivatedPayload;
import com.tubetasks.notification.event.CampaignNotificationPayload;
import com.tubetasks.notification.event.EmailVerificationRequestedPayload;
import com.tubetasks.notification.event.NotificationEvent;
import com.tubetasks.notification.event.TransactionNotificationPayload;
import com.tubetasks.notification.persistence.DeliveryRepository;
import com.tubetasks.notification.persistence.ProcessedEventRepository;
import com.tubetasks.notification.persistence.ProcessedEventStatus;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest(properties = "notification-service.send-enabled=true")
@Import({TestChannelBinderConfiguration.class, TestJwtDecoderConfig.class})
class NotificationDispatchIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(new ServerSetup(3025, "localhost", ServerSetup.PROTOCOL_SMTP));

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @BeforeEach
    void setUp() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        deliveryRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void consumesVerificationEventAndSendsOneEmail() throws Exception {
        NotificationEvent event = verificationEvent("event-1", "token-1");
        inputDestination.send(
                MessageBuilder.withPayload(event)
                        .setHeader("serviceRequestId", "req-1")
                        .build(),
                "tubetasks.notification.events");

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("Verify your TubeTasks email");
        assertThat(processedEventRepository.findByEventId("event-1"))
                .isPresent()
                .get()
                .extracting(entity -> entity.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
    }

    @Test
    void duplicateEventIdDoesNotSendSecondEmail() {
        NotificationEvent first = verificationEvent("event-dup", "token-dup");
        NotificationEvent second = verificationEvent("event-dup", "token-dup");
        inputDestination.send(MessageBuilder.withPayload(first).build(), "tubetasks.notification.events");
        inputDestination.send(MessageBuilder.withPayload(second).build(), "tubetasks.notification.events");
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    void sameTokenWithDifferentEventIdDoesNotSendSecondEmail() {
        inputDestination.send(
                MessageBuilder.withPayload(verificationEvent("event-a", "same-token"))
                        .build(),
                "tubetasks.notification.events");
        inputDestination.send(
                MessageBuilder.withPayload(verificationEvent("event-b", "same-token"))
                        .build(),
                "tubetasks.notification.events");
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    void newTokenSendsSecondEmail() {
        inputDestination.send(
                MessageBuilder.withPayload(verificationEvent("event-1b", "token-1"))
                        .build(),
                "tubetasks.notification.events");
        inputDestination.send(
                MessageBuilder.withPayload(verificationEvent("event-2b", "token-2"))
                        .build(),
                "tubetasks.notification.events");
        assertThat(greenMail.getReceivedMessages()).hasSize(2);
    }

    @Test
    void consumesWelcomeEventAndSendsOneEmail() throws Exception {
        NotificationEvent event = welcomeEvent("event-welcome");
        inputDestination.send(MessageBuilder.withPayload(event).build(), "tubetasks.notification.events");

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("Welcome to TubeTasks");
        assertThat(processedEventRepository.findByEventId("event-welcome"))
                .isPresent()
                .get()
                .extracting(entity -> entity.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
    }

    @Test
    void consumesPaymentSubmittedEventAndSendsOneEmail() throws Exception {
        NotificationEvent event = paymentSubmittedEvent("event-pay-1", "txn-1");
        inputDestination.send(MessageBuilder.withPayload(event).build(), "tubetasks.notification.events");

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("deposit request");
        assertThat(processedEventRepository.findByEventId("event-pay-1"))
                .isPresent()
                .get()
                .extracting(entity -> entity.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
    }

    @Test
    void consumesWithdrawalRejectedEventAndSendsOneEmail() throws Exception {
        NotificationEvent event = withdrawalRejectedEvent("event-wd-rej", "txn-wd-1");
        inputDestination.send(MessageBuilder.withPayload(event).build(), "tubetasks.notification.events");

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("withdrawal was not approved");
    }

    @Test
    void consumesSubscriptionPurchasedEventAndSendsOneEmail() throws Exception {
        NotificationEvent event = subscriptionPurchasedEvent("event-sub-1", "purchase-1");
        inputDestination.send(MessageBuilder.withPayload(event).build(), "tubetasks.notification.events");

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("campaign is active");
        assertThat(processedEventRepository.findByEventId("event-sub-1"))
                .isPresent()
                .get()
                .extracting(entity -> entity.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
    }

    @Test
    void consumesCampaignCompletedEventAndSendsOneEmail() throws Exception {
        NotificationEvent event = campaignCompletedEvent("event-camp-1", "purchase-1");
        inputDestination.send(MessageBuilder.withPayload(event).build(), "tubetasks.notification.events");

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).contains("campaign is complete");
    }

    private static NotificationEvent verificationEvent(String eventId, String token) {
        EmailVerificationRequestedPayload payload = new EmailVerificationRequestedPayload(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                token,
                "http://localhost:9000/auth/api/v1/auth/register/verify?token=" + token,
                Instant.now().plusSeconds(3600));
        return new NotificationEvent(
                eventId,
                "EMAIL_VERIFICATION_REQUESTED",
                1,
                Instant.now(),
                "auth-server",
                "req-test",
                payload);
    }

    private static NotificationEvent welcomeEvent(String eventId) {
        AccountActivatedPayload payload = new AccountActivatedPayload(
                "user-123", "Jane Doe", "jane@example.com", "http://localhost:9000/auth/login");
        return new NotificationEvent(
                eventId, "ACCOUNT_ACTIVATED", 1, Instant.now(), "auth-server", "req-test", payload);
    }

    private static NotificationEvent paymentSubmittedEvent(String eventId, String transactionId) {
        TransactionNotificationPayload payload = new TransactionNotificationPayload(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                transactionId,
                "100.0000",
                "INR",
                "PENDING",
                "CREDIT",
                null,
                null);
        return new NotificationEvent(
                eventId, "PAYMENT_SUBMITTED", 1, Instant.now(), "user-service", "req-test", payload);
    }

    private static NotificationEvent withdrawalRejectedEvent(String eventId, String transactionId) {
        TransactionNotificationPayload payload = new TransactionNotificationPayload(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                transactionId,
                "50.0000",
                "INR",
                "REJECTED",
                "DEBIT",
                "Invalid UPI",
                "jane@oksbi");
        return new NotificationEvent(
                eventId, "WITHDRAWAL_REJECTED", 1, Instant.now(), "user-service", "req-test", payload);
    }

    private static NotificationEvent subscriptionPurchasedEvent(String eventId, String purchaseId) {
        CampaignNotificationPayload payload = new CampaignNotificationPayload(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                purchaseId,
                null,
                "Starter",
                "My Channel",
                "199.0000",
                "INR",
                "ACTIVE",
                null);
        return new NotificationEvent(
                eventId, "SUBSCRIPTION_PURCHASED", 1, Instant.now(), "task-service", "req-test", payload);
    }

    private static NotificationEvent campaignCompletedEvent(String eventId, String purchaseId) {
        CampaignNotificationPayload payload = new CampaignNotificationPayload(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                purchaseId,
                null,
                "Starter",
                "My Channel",
                "199.0000",
                "INR",
                "COMPLETED",
                null);
        return new NotificationEvent(
                eventId, "CAMPAIGN_COMPLETED", 1, Instant.now(), "task-service", "req-test", payload);
    }
}
