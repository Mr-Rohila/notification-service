package com.tubetasks.notification.client;

import com.tubetasks.notification.common.NotificationServiceProperties;
import com.tubetasks.notification.common.ServiceRequestIdFilter;
import com.tubetasks.notification.persistence.DeliveryEntity;
import com.tubetasks.notification.persistence.DeliveryRepository;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BroadcastDeliveryCallback {

    private static final Logger log = LoggerFactory.getLogger(BroadcastDeliveryCallback.class);
    private static final int MAX_ATTEMPTS = 8;

    private final RestClient restClient;
    private final NotificationAuthTokenClient tokenClient;
    private final NotificationServiceProperties properties;
    private final DeliveryRepository deliveryRepository;

    public BroadcastDeliveryCallback(
            RestClient.Builder builder,
            NotificationAuthTokenClient tokenClient,
            NotificationServiceProperties properties,
            DeliveryRepository deliveryRepository) {
        this.restClient = builder.build();
        this.tokenClient = tokenClient;
        this.properties = properties;
        this.deliveryRepository = deliveryRepository;
    }

    public void pendingAfterSend(String eventId, String errorCode) {
        DeliveryEntity delivery = deliveryRepository.findFirstByEventId(eventId).orElse(null);
        if (delivery == null) {
            return;
        }
        if ("DONE".equals(delivery.getCallbackStatus())) {
            return;
        }
        delivery.setCallbackStatus("PENDING");
        if (errorCode != null) {
            delivery.setErrorCode(errorCode);
        }
        deliveryRepository.save(delivery);
        String status = delivery.getStatus();
        dispatch(eventId, status, delivery.getErrorCode());
    }

    public void dispatch(String eventId, String status, String errorCode) {
        DeliveryEntity delivery = deliveryRepository.findFirstByEventId(eventId).orElse(null);
        if (delivery == null || "DONE".equals(delivery.getCallbackStatus())) {
            return;
        }
        try {
            String base = properties.getUserService().getBaseUrl();
            String path = properties.getUserService().getDeliveryPath();
            String url = base.endsWith("/") ? base.substring(0, base.length() - 1) + path : base + path;
            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("eventId", eventId);
            body.put("status", status);
            if (errorCode != null && !errorCode.isBlank()) {
                body.put("errorCode", errorCode);
            }
            restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + tokenClient.accessToken())
                    .header(
                            ServiceRequestIdFilter.SERVICE_REQUEST_ID_HEADER,
                            MDC.get(ServiceRequestIdFilter.SERVICE_REQUEST_ID))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            delivery.setCallbackStatus("DONE");
            deliveryRepository.save(delivery);
        } catch (RuntimeException ex) {
            int attempts = delivery.getCallbackAttempts() + 1;
            delivery.setCallbackAttempts(attempts);
            delivery.setCallbackStatus(attempts >= MAX_ATTEMPTS ? "FAILED" : "PENDING");
            deliveryRepository.save(delivery);
            log.warn("operation=broadcast_callback eventId={} outcome=retry attempt={}", eventId, attempts);
        }
    }

    @Scheduled(fixedDelayString = "${notification-service.callback-retry-delay:30s}")
    public void retryPending() {
        for (DeliveryEntity delivery :
                deliveryRepository.findByCallbackStatusAndCallbackAttemptsLessThan("PENDING", MAX_ATTEMPTS)) {
            dispatch(delivery.getEventId(), delivery.getStatus(), delivery.getErrorCode());
        }
    }
}
