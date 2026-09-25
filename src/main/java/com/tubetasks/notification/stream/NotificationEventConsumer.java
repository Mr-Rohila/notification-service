package com.tubetasks.notification.stream;

import com.tubetasks.notification.api.exception.RetryableNotificationException;
import com.tubetasks.notification.client.BroadcastDeliveryCallback;
import com.tubetasks.notification.common.NotificationServiceProperties;
import com.tubetasks.notification.common.ServiceRequestIdFilter;
import com.tubetasks.notification.event.NotificationEvent;
import com.tubetasks.notification.service.NotificationDispatchService;
import java.util.function.Consumer;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("notificationEvents")
public class NotificationEventConsumer implements Consumer<Message<NotificationEvent>> {

    private final NotificationDispatchService dispatchService;
    private final NotificationServiceProperties properties;
    private final BroadcastDeliveryCallback broadcastCallback;

    public NotificationEventConsumer(
            NotificationDispatchService dispatchService,
            NotificationServiceProperties properties,
            BroadcastDeliveryCallback broadcastCallback) {
        this.dispatchService = dispatchService;
        this.properties = properties;
        this.broadcastCallback = broadcastCallback;
    }

    @Override
    public void accept(Message<NotificationEvent> message) {
        if (!properties.isConsumerEnabled()) {
            return;
        }
        NotificationEvent event = message.getPayload();
        String serviceRequestId = firstNonBlank(
                message.getHeaders().get("serviceRequestId", String.class),
                event != null ? event.serviceRequestId() : null);
        if (!StringUtils.hasText(serviceRequestId)) {
            serviceRequestId = NotificationDispatchService.resolveServiceRequestId(null);
        }
        MDC.put(ServiceRequestIdFilter.SERVICE_REQUEST_ID, serviceRequestId);
        try {
            dispatchService.dispatchFromKafka(event);
        } catch (RetryableNotificationException ex) {
            if (event != null
                    && "ADMIN_BROADCAST".equals(event.eventType())
                    && deliveryAttempt(message) >= 3) {
                broadcastCallback.pendingAfterSend(event.eventId(), "SMTP_FAILED");
            }
            throw ex;
        } finally {
            MDC.remove(ServiceRequestIdFilter.SERVICE_REQUEST_ID);
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
    }

    private static int deliveryAttempt(Message<NotificationEvent> message) {
        Object value = message.getHeaders().get("deliveryAttempt");
        if (!(value instanceof Number)) {
            value = message.getHeaders().get("kafka_deliveryAttempt");
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 1;
    }
}
