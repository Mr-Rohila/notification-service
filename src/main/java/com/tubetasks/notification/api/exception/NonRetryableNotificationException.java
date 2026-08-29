package com.tubetasks.notification.api.exception;

public class NonRetryableNotificationException extends RuntimeException {

    public NonRetryableNotificationException(String message) {
        super(message);
    }
}
