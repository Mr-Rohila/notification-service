package com.tubetasks.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.tubetasks.notification.config.TestJwtDecoderConfig;
import com.tubetasks.notification.event.EmailVerificationRequestedPayload;
import com.tubetasks.notification.event.NotificationEvent;
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

@SpringBootTest
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

    private static NotificationEvent verificationEvent(String eventId, String token) {
        EmailVerificationRequestedPayload payload = new EmailVerificationRequestedPayload(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                token,
                "http://localhost:9000/api/v1/auth/register/verify?token=" + token,
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
}
