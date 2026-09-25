package com.tubetasks.notification.event;

public record AdminBroadcastPayload(
        String userId,
        String displayName,
        String email,
        String subject,
        String bodyText,
        String eventToken) {}
