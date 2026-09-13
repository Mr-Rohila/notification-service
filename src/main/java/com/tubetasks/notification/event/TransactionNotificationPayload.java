package com.tubetasks.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionNotificationPayload(
        String userId,
        String displayName,
        String email,
        String transactionId,
        String amount,
        String currency,
        String status,
        String type,
        String rejectReason,
        String upiId) {}
