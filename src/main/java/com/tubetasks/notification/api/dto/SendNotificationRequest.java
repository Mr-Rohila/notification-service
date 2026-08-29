package com.tubetasks.notification.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "Internal service-to-service notification send request")
public record SendNotificationRequest(
        @NotBlank @Schema(description = "Notification event type") String eventType,
        @Schema(description = "Optional idempotency event identifier") String eventId,
        @Schema(description = "Public user identifier") String userId,
        @NotNull @Valid @Schema(description = "Event-specific payload") Map<String, Object> payload) {}
