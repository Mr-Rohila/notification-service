package com.tubetasks.notification.api;

import com.tubetasks.notification.api.dto.SendNotificationRequest;
import com.tubetasks.notification.api.dto.SendNotificationResponse;
import com.tubetasks.notification.common.ServiceRequestIdFilter;
import com.tubetasks.notification.event.NotificationEvent;
import com.tubetasks.notification.service.NotificationDispatchService;
import com.tubetasks.notification.service.NotificationDispatchService.DispatchStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/notifications")
@Tag(name = "Internal Notifications")
@SecurityRequirement(name = "bearerAuth")
public class InternalNotificationController {

    private final NotificationDispatchService dispatchService;

    public InternalNotificationController(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send a notification email synchronously")
    public ResponseEntity<SendNotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        String eventId = StringUtils.hasText(request.eventId()) ? request.eventId() : UUID.randomUUID().toString();
        String serviceRequestId = MDC.get(ServiceRequestIdFilter.MDC_KEY);
        NotificationEvent event = new NotificationEvent(
                eventId,
                request.eventType(),
                1,
                Instant.now(),
                "internal-api",
                serviceRequestId,
                request.payload());
        NotificationDispatchService.DispatchOutcome outcome = dispatchService.dispatchFromRest(event);
        String status = mapStatus(outcome.status());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new SendNotificationResponse(outcome.eventId(), status, outcome.serviceRequestId()));
    }

    private static String mapStatus(DispatchStatus status) {
        return status == DispatchStatus.DUPLICATE ? "DUPLICATE" : status.name();
    }
}
