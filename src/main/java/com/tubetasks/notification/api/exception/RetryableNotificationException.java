package com.tubetasks.notification.api.exception;

public class RetryableNotificationException extends RuntimeException {

    public RetryableNotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableNotificationException(String message) {
        super(message);
    }
}
