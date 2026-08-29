package com.tubetasks.notification.common;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskingConverterTests {

    private final SensitiveDataMaskingConverter converter = new SensitiveDataMaskingConverter();

    @Test
    void masksVerificationTokenInLogs() {
        String masked = converter.convert(loggingEvent("verificationToken=secret-token-value"));
        assertThat(masked).doesNotContain("secret-token-value");
        assertThat(masked).contains("verificationToken=***");
    }

    @Test
    void masksResetTokenQueryParameter() {
        String masked = converter.convert(loggingEvent("url=http://localhost/reset?resetToken=abc123"));
        assertThat(masked).doesNotContain("abc123");
    }

    private static LoggingEvent loggingEvent(String message) {
        LoggerContext context = new LoggerContext();
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setMessage(message);
        return event;
    }
}
