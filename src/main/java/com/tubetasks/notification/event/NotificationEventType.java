package com.tubetasks.notification.event;

public enum NotificationEventType {
    EMAIL_VERIFICATION_REQUESTED,
    PASSWORD_RESET_REQUESTED;

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
