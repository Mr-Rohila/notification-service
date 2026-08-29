package com.tubetasks.notification.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Internal notification send response")
public record SendNotificationResponse(
        @Schema(description = "Event identifier") String eventId,
        @Schema(description = "Delivery status") String status,
        @Schema(description = "Service request identifier") String serviceRequestId) {}
