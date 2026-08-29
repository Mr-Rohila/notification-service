package com.tubetasks.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordResetRequestedPayload(
        String userId,
        String displayName,
        String email,
        String resetToken,
        String resetUrl,
        Instant tokenExpiresAt) {}
