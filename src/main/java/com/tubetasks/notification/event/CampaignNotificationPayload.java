package com.tubetasks.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignNotificationPayload(
        String userId,
        String displayName,
        String email,
        String purchaseId,
        String taskId,
        String planTitle,
        String channelTitle,
        String amount,
        String currency,
        String status,
        String rejectReason) {}
