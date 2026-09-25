package com.tubetasks.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.tubetasks.notification.client.NotificationAuthTokenClient;
import com.tubetasks.notification.common.NotificationServiceProperties;
import com.tubetasks.notification.config.TestJwtDecoderConfig;
import com.tubetasks.notification.event.AdminBroadcastPayload;
import com.tubetasks.notification.event.NotificationEvent;
import com.tubetasks.notification.persistence.DeliveryRepository;
import com.tubetasks.notification.persistence.ProcessedEventRepository;
import com.tubetasks.notification.persistence.ProcessedEventStatus;
import com.tubetasks.notification.service.NotificationDispatchService.DispatchStatus;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import({TestChannelBinderConfiguration.class, TestJwtDecoderConfig.class})
class AdminBroadcastMailTests {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(new ServerSetup(3025, "localhost", ServerSetup.PROTOCOL_SMTP));

    @Autowired
    private NotificationDispatchService dispatchService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private NotificationServiceProperties properties;

    @Autowired
    private com.tubetasks.notification.client.BroadcastDeliveryCallback broadcastDeliveryCallback;

    @MockitoBean
    private NotificationAuthTokenClient tokenClient;

    private HttpServer callbackServer;
    private final AtomicReference<String> callbackBody = new AtomicReference<>();
    private final AtomicInteger callbackStatus = new AtomicInteger(200);
    private boolean originalSendEnabled;
    private String originalBaseUrl;

    @BeforeEach
    void setUp() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        deliveryRepository.deleteAll();
        processedEventRepository.deleteAll();
        when(tokenClient.accessToken()).thenReturn("test-token");
        originalSendEnabled = properties.isSendEnabled();
        originalBaseUrl = properties.getUserService().getBaseUrl();
        callbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        callbackServer.createContext("/internal/api/v1/broadcasts/delivery", exchange -> {
            callbackBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int status = callbackStatus.get();
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        callbackServer.start();
        properties.getUserService().setBaseUrl("http://127.0.0.1:" + callbackServer.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        properties.setSendEnabled(originalSendEnabled);
        properties.getUserService().setBaseUrl(originalBaseUrl);
        if (callbackServer != null) {
            callbackServer.stop(0);
        }
    }

    @Test
    void sendDisabledSkipsSmtpAndReportsSendDisabled() {
        properties.setSendEnabled(false);
        NotificationDispatchService.DispatchOutcome outcome =
                dispatchService.dispatchFromKafka(broadcast("skip-1", "Hello", "Line one\n<script>"));

        assertThat(outcome.status()).isEqualTo(DispatchStatus.SKIPPED);
        assertThat(greenMail.getReceivedMessages()).isEmpty();
        assertThat(processedEventRepository.findByEventId("skip-1"))
                .get()
                .extracting(row -> row.getStatus())
                .isEqualTo(ProcessedEventStatus.SKIPPED);
        assertThat(callbackBody.get()).contains("SEND_DISABLED").contains("SKIPPED");
    }

    @Test
    void payloadSubjectIsUsedAndADuplicateEventDoesNotSendTwice() throws Exception {
        properties.setSendEnabled(true);
        dispatchService.dispatchFromKafka(broadcast("send-1", "Balance for Ada", "Hello\n<script>alert(1)</script>"));
        NotificationDispatchService.DispatchOutcome duplicate =
                dispatchService.dispatchFromKafka(broadcast("send-1", "Balance for Ada", "Hello\n<script>alert(1)</script>"));

        assertThat(duplicate.status()).isEqualTo(DispatchStatus.DUPLICATE);
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).isEqualTo("Balance for Ada");
        String raw = messageText(message);
        assertThat(raw).contains("&lt;script&gt;");
        assertThat(raw).doesNotContain("<script>");
    }

    private static String messageText(MimeMessage message) throws Exception {
        return messageText(message.getContent());
    }

    private static String messageText(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof MimeMultipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                builder.append(messageText(multipart.getBodyPart(i).getContent()));
            }
            return builder.toString();
        }
        return String.valueOf(content);
    }

    @Test
    void callbackFailureLeavesSentAndRetryMarksDone() {
        properties.setSendEnabled(true);
        callbackStatus.set(500);
        dispatchService.dispatchFromKafka(broadcast("cb-1", "Hello Ada", "Body"));

        assertThat(processedEventRepository.findByEventId("cb-1"))
                .get()
                .extracting(row -> row.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
        assertThat(deliveryRepository.findFirstByEventId("cb-1"))
                .get()
                .extracting(row -> row.getCallbackStatus())
                .isEqualTo("PENDING");

        callbackStatus.set(200);
        broadcastDeliveryCallback.retryPending();

        assertThat(processedEventRepository.findByEventId("cb-1"))
                .get()
                .extracting(row -> row.getStatus())
                .isEqualTo(ProcessedEventStatus.SENT);
        assertThat(deliveryRepository.findFirstByEventId("cb-1"))
                .get()
                .extracting(row -> row.getCallbackStatus())
                .isEqualTo("DONE");
    }

    private static NotificationEvent broadcast(String eventId, String subject, String body) {
        return new NotificationEvent(
                eventId,
                "ADMIN_BROADCAST",
                1,
                Instant.now(),
                "user-service",
                "req-" + eventId,
                new AdminBroadcastPayload("user-1", "Ada", "ada@example.com", subject, body, eventId));
    }
}
