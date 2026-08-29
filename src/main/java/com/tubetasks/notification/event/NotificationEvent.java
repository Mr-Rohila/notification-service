package com.tubetasks.notification.event;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationEvent(
        String eventId,
        String eventType,
        int eventVersion,
        java.time.Instant occurredAt,
        String source,
        String serviceRequestId,
        Object payload) {}
