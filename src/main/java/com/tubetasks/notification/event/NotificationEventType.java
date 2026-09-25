package com.tubetasks.notification.event;

public enum NotificationEventType {
    EMAIL_VERIFICATION_REQUESTED,
    PASSWORD_RESET_REQUESTED,
    ACCOUNT_ACTIVATED,
    PAYMENT_SUBMITTED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED,
    WITHDRAWAL_CREATED,
    WITHDRAWAL_APPROVED,
    WITHDRAWAL_REJECTED,
    SUBSCRIPTION_PURCHASED,
    CAMPAIGN_COMPLETED,
    TASK_ASSIGNED,
    ADMIN_WALLET_CREDITED,
    REFERRAL_REWARD_CREDITED,
    ADMIN_BROADCAST;

    public static boolean isKnown(String eventType) {
        if (eventType == null) {
            return false;
        }
        try {
            valueOf(eventType);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
