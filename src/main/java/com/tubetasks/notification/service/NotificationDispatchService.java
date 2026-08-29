package com.tubetasks.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubetasks.notification.api.exception.EmailDeliveryException;
import com.tubetasks.notification.api.exception.NonRetryableNotificationException;
import com.tubetasks.notification.api.exception.RetryableNotificationException;
import com.tubetasks.notification.api.exception.ValidationException;
import com.tubetasks.notification.common.DisplayNameResolver;
import com.tubetasks.notification.event.EmailVerificationRequestedPayload;
import com.tubetasks.notification.event.NotificationEvent;
import com.tubetasks.notification.event.NotificationEventType;
import com.tubetasks.notification.event.PasswordResetRequestedPayload;
import com.tubetasks.notification.mail.MailComposer;
import com.tubetasks.notification.mail.SmtpMailSender;
import com.tubetasks.notification.mail.TemplateRegistry;
import com.tubetasks.notification.observability.NotificationMetrics;
import com.tubetasks.notification.persistence.DeliveryEntity;
import com.tubetasks.notification.persistence.DeliveryRepository;
import com.tubetasks.notification.persistence.ProcessedEventEntity;
import com.tubetasks.notification.persistence.ProcessedEventRepository;
import com.tubetasks.notification.persistence.ProcessedEventStatus;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);
    private static final int EVENT_VERSION_V1 = 1;

    private final ObjectMapper objectMapper;
    private final TemplateRegistry templateRegistry;
    private final MailComposer mailComposer;
    private SmtpMailSender smtpMailSender;
    private final ProcessedEventRepository processedEventRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationMetrics notificationMetrics;

    public NotificationDispatchService(
            ObjectMapper objectMapper,
            TemplateRegistry templateRegistry,
            MailComposer mailComposer,
            SmtpMailSender smtpMailSender,
            ProcessedEventRepository processedEventRepository,
            DeliveryRepository deliveryRepository,
            NotificationMetrics notificationMetrics) {
        this.objectMapper = objectMapper;
        this.templateRegistry = templateRegistry;
        this.mailComposer = mailComposer;
        this.smtpMailSender = smtpMailSender;
        this.processedEventRepository = processedEventRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationMetrics = notificationMetrics;
    }

    public DispatchOutcome dispatchFromKafka(NotificationEvent event) {
        validateEnvelope(event);
        if (!templateRegistry.isKnownEventType(event.eventType())) {
            log.info(
                    "Skipping unknown eventType={} eventId={}",
                    event.eventType(),
                    event.eventId());
            notificationMetrics.recordConsumed(event.eventType(), "skipped_unknown");
            return new DispatchOutcome(event.eventId(), DispatchStatus.SKIPPED_UNKNOWN, event.serviceRequestId());
        }
        return dispatchKnownEvent(event);
    }

    public DispatchOutcome dispatchFromRest(NotificationEvent event) {
        validateEnvelope(event);
        if (!templateRegistry.isKnownEventType(event.eventType())) {
            throw new com.tubetasks.notification.api.exception.UnsupportedEventTypeException(
                    "Unsupported event type: " + event.eventType());
        }
        return dispatchKnownEvent(event);
    }

    private DispatchOutcome dispatchKnownEvent(NotificationEvent event) {
        if (event.eventVersion() != EVENT_VERSION_V1) {
            notificationMetrics.recordDlt(event.eventType());
            throw new NonRetryableNotificationException(
                    "Unsupported event version for type " + event.eventType() + ": " + event.eventVersion());
        }

        ParsedNotification parsed = parsePayload(event);
        if (parsed.tokenExpiresAt() != null && !parsed.tokenExpiresAt().isAfter(Instant.now())) {
            persistTerminalState(event, parsed, ProcessedEventStatus.EXPIRED, null);
            log.warn(
                    "Expired token for eventId={} eventType={} userId={}",
                    event.eventId(),
                    event.eventType(),
                    parsed.userId());
            notificationMetrics.recordConsumed(event.eventType(), "expired");
            return new DispatchOutcome(event.eventId(), DispatchStatus.EXPIRED, event.serviceRequestId());
        }

        String businessKey = computeBusinessKey(event.eventType(), parsed.userId(), parsed.rawToken());
        Optional<ProcessedEventEntity> existingEventId = processedEventRepository.findByEventId(event.eventId());
        if (existingEventId.isPresent()) {
            notificationMetrics.recordConsumed(event.eventType(), "duplicate");
            return new DispatchOutcome(event.eventId(), DispatchStatus.DUPLICATE, event.serviceRequestId());
        }

        Optional<ProcessedEventEntity> existingBusinessKey =
                processedEventRepository.findByBusinessKey(businessKey);
        ProcessedEventEntity processing;
        if (existingBusinessKey.isPresent()) {
            ProcessedEventEntity existing = existingBusinessKey.get();
            ProcessedEventStatus status = existing.getStatus();
            if (status == ProcessedEventStatus.SENT
                    || status == ProcessedEventStatus.SKIPPED
                    || status == ProcessedEventStatus.EXPIRED) {
                notificationMetrics.recordConsumed(event.eventType(), "duplicate");
                return new DispatchOutcome(event.eventId(), DispatchStatus.DUPLICATE, event.serviceRequestId());
            }
            existing.setStatus(ProcessedEventStatus.PROCESSING);
            processing = processedEventRepository.save(existing);
        } else {
            processing = reserveProcessing(event, parsed, businessKey);
        }
        mailComposer.validateActionUrl(parsed.actionUrl());
        TemplateRegistry.TemplateDefinition template = templateRegistry.resolve(event.eventType());
        String displayName = DisplayNameResolver.resolve(parsed.displayName(), parsed.email());
        MailComposer.ComposedMail composed =
                mailComposer.compose(template, displayName, parsed.email(), parsed.actionUrl(), parsed.tokenExpiresAt());

        Timer.Sample sample = notificationMetrics.startSendTimer();
        try {
            smtpMailSender.send(composed);
        } catch (EmailDeliveryException ex) {
            notificationMetrics.recordSmtpError(ex.getClass().getSimpleName());
            markFailed(processing, ex.getMessage());
            throw new RetryableNotificationException("SMTP delivery failed", ex);
        } finally {
            notificationMetrics.stopSendTimer(sample, template.templateName());
        }

        markSent(processing, composed);
        log.info(
                "Sent email eventId={} eventType={} userId={} template={} outcome=sent",
                event.eventId(),
                event.eventType(),
                parsed.userId(),
                template.templateName());
        notificationMetrics.recordConsumed(event.eventType(), "sent");
        return new DispatchOutcome(event.eventId(), DispatchStatus.SENT, event.serviceRequestId());
    }

    private void validateEnvelope(NotificationEvent event) {
        if (event == null) {
            throw new ValidationException("Event is required");
        }
        if (!StringUtils.hasText(event.eventId())) {
            throw new ValidationException("eventId is required");
        }
        if (!StringUtils.hasText(event.eventType())) {
            throw new ValidationException("eventType is required");
        }
        if (event.payload() == null) {
            throw new ValidationException("payload is required");
        }
    }

    private ParsedNotification parsePayload(NotificationEvent event) {
        try {
            if (NotificationEventType.EMAIL_VERIFICATION_REQUESTED.name().equals(event.eventType())) {
                EmailVerificationRequestedPayload payload =
                        objectMapper.convertValue(event.payload(), EmailVerificationRequestedPayload.class);
                validateCommonPayload(
                        payload.email(),
                        payload.userId(),
                        payload.verificationUrl(),
                        payload.verificationToken(),
                        payload.tokenExpiresAt());
                return new ParsedNotification(
                        payload.userId(),
                        payload.displayName(),
                        payload.email(),
                        payload.verificationToken(),
                        payload.verificationUrl(),
                        payload.tokenExpiresAt());
            }
            if (NotificationEventType.PASSWORD_RESET_REQUESTED.name().equals(event.eventType())) {
                PasswordResetRequestedPayload payload =
                        objectMapper.convertValue(event.payload(), PasswordResetRequestedPayload.class);
                validateCommonPayload(
                        payload.email(),
                        payload.userId(),
                        payload.resetUrl(),
                        payload.resetToken(),
                        payload.tokenExpiresAt());
                return new ParsedNotification(
                        payload.userId(),
                        payload.displayName(),
                        payload.email(),
                        payload.resetToken(),
                        payload.resetUrl(),
                        payload.tokenExpiresAt());
            }
            throw new RetryableNotificationException("Unsupported known event type during parse: " + event.eventType());
        } catch (ValidationException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new RetryableNotificationException("Invalid payload for event type " + event.eventType(), ex);
        }
    }

    private void validateCommonPayload(
            String email, String userId, String actionUrl, String rawToken, Instant tokenExpiresAt) {
        if (!StringUtils.hasText(email) || email.length() > 320) {
            throw new ValidationException("payload.email is required and must be at most 320 characters");
        }
        if (!StringUtils.hasText(userId)) {
            throw new ValidationException("payload.userId is required");
        }
        if (!StringUtils.hasText(actionUrl)) {
            throw new ValidationException("Action URL is required");
        }
        if (!StringUtils.hasText(rawToken)) {
            throw new ValidationException("Token is required");
        }
        if (tokenExpiresAt == null) {
            throw new ValidationException("tokenExpiresAt is required");
        }
    }

    @Transactional
    protected ProcessedEventEntity reserveProcessing(
            NotificationEvent event, ParsedNotification parsed, String businessKey) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(event.eventId());
        entity.setBusinessKey(businessKey);
        entity.setEventType(event.eventType());
        entity.setEventVersion(event.eventVersion());
        entity.setUserId(parsed.userId());
        entity.setSource(event.source());
        entity.setServiceRequestId(resolveServiceRequestId(event.serviceRequestId()));
        entity.setStatus(ProcessedEventStatus.PROCESSING);
        return processedEventRepository.save(entity);
    }

    @Transactional
    protected void markSent(ProcessedEventEntity processing, MailComposer.ComposedMail composed) {
        processing.setStatus(ProcessedEventStatus.SENT);
        processedEventRepository.save(processing);
        DeliveryEntity delivery = new DeliveryEntity();
        delivery.setEventId(processing.getEventId());
        delivery.setTemplate(composed.template().templateName());
        delivery.setRecipientEmail(composed.recipientEmail());
        delivery.setSubject(composed.template().subject());
        delivery.setStatus(ProcessedEventStatus.SENT.name());
        delivery.setServiceRequestId(processing.getServiceRequestId());
        delivery.setSentAt(Instant.now());
        deliveryRepository.save(delivery);
    }

    @Transactional
    protected void markFailed(ProcessedEventEntity processing, String errorMessage) {
        processing.setStatus(ProcessedEventStatus.FAILED);
        processedEventRepository.save(processing);
        DeliveryEntity delivery = new DeliveryEntity();
        delivery.setEventId(processing.getEventId());
        delivery.setTemplate("unknown");
        delivery.setRecipientEmail("unknown");
        delivery.setSubject("failed");
        delivery.setStatus(ProcessedEventStatus.FAILED.name());
        delivery.setErrorCode("EMAIL_DELIVERY_FAILED");
        delivery.setErrorMessage(truncate(errorMessage, 512));
        delivery.setServiceRequestId(processing.getServiceRequestId());
        deliveryRepository.save(delivery);
        notificationMetrics.recordDlt(processing.getEventType());
    }

    @Transactional
    protected void persistTerminalState(
            NotificationEvent event,
            ParsedNotification parsed,
            ProcessedEventStatus status,
            MailComposer.ComposedMail composed) {
        if (processedEventRepository.findByEventId(event.eventId()).isPresent()) {
            return;
        }
        String businessKey = computeBusinessKey(event.eventType(), parsed.userId(), parsed.rawToken());
        if (processedEventRepository.findByBusinessKey(businessKey).isPresent()) {
            return;
        }
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(event.eventId());
        entity.setBusinessKey(businessKey);
        entity.setEventType(event.eventType());
        entity.setEventVersion(event.eventVersion());
        entity.setUserId(parsed.userId());
        entity.setSource(event.source());
        entity.setServiceRequestId(resolveServiceRequestId(event.serviceRequestId()));
        entity.setStatus(status);
        processedEventRepository.save(entity);
        if (composed != null) {
            DeliveryEntity delivery = new DeliveryEntity();
            delivery.setEventId(entity.getEventId());
            delivery.setTemplate(composed.template().templateName());
            delivery.setRecipientEmail(composed.recipientEmail());
            delivery.setSubject(composed.template().subject());
            delivery.setStatus(status.name());
            delivery.setServiceRequestId(entity.getServiceRequestId());
            if (status == ProcessedEventStatus.SENT) {
                delivery.setSentAt(Instant.now());
            }
            deliveryRepository.save(delivery);
        }
    }

    static String computeBusinessKey(String eventType, String userId, String rawToken) {
        try {
            String material = eventType + "|" + userId + "|" + rawToken;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute business key", ex);
        }
    }

    public static String resolveServiceRequestId(String serviceRequestId) {
        if (StringUtils.hasText(serviceRequestId)) {
            return serviceRequestId;
        }
        return UUID.randomUUID().toString();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record ParsedNotification(
            String userId,
            String displayName,
            String email,
            String rawToken,
            String actionUrl,
            Instant tokenExpiresAt) {}

    public enum DispatchStatus {
        SENT,
        DUPLICATE,
        EXPIRED,
        SKIPPED_UNKNOWN
    }

    public record DispatchOutcome(String eventId, DispatchStatus status, String serviceRequestId) {}
}
