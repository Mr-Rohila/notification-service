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
import com.tubetasks.notification.service.NotificationDispatchService.DispatchStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "notification-service.send-enabled=false")
@Import({TestChannelBinderConfiguration.class, TestJwtDecoderConfig.class})
class NotificationDispatchBypassIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(new ServerSetup(3025, "localhost", ServerSetup.PROTOCOL_SMTP));

    @Autowired
    private NotificationDispatchService dispatchService;

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
    void marksSentWithoutDeliveringEmailWhenSendDisabled() {
        NotificationEvent event = verificationEvent("event-bypass", "token-bypass");

        NotificationDispatchService.DispatchOutcome outcome = dispatchService.dispatchFromKafka(event);

        assertThat(outcome.status()).isEqualTo(DispatchStatus.SENT);
        assertThat(greenMail.getReceivedMessages()).isEmpty();
        assertThat(processedEventRepository.findByEventId("event-bypass"))
                .isPresent()
                .get()
                .extracting(entity -> entity.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
        assertThat(deliveryRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(delivery -> {
                    assertThat(delivery.getEventId()).isEqualTo("event-bypass");
                    assertThat(delivery.getStatus()).isEqualTo(ProcessedEventStatus.SENT.name());
                    assertThat(delivery.getRecipientEmail()).isEqualTo("jane@example.com");
                    assertThat(delivery.getSentAt()).isNotNull();
                });
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
