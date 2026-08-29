package com.tubetasks.notification.stream;

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

    public NotificationEventConsumer(
            NotificationDispatchService dispatchService, NotificationServiceProperties properties) {
        this.dispatchService = dispatchService;
        this.properties = properties;
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
        MDC.put(ServiceRequestIdFilter.MDC_KEY, serviceRequestId);
        try {
            dispatchService.dispatchFromKafka(event);
        } finally {
            MDC.remove(ServiceRequestIdFilter.MDC_KEY);
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
    }
}
