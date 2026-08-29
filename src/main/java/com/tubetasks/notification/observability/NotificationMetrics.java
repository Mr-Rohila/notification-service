package com.tubetasks.notification.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordConsumed(String eventType, String outcome) {
        Counter.builder("notification_events_consumed_total")
                .tag("eventType", safeTag(eventType))
                .tag("outcome", safeTag(outcome))
                .register(meterRegistry)
                .increment();
    }

    public void recordDlt(String eventType) {
        Counter.builder("notification_dlt_total")
                .tag("eventType", safeTag(eventType))
                .register(meterRegistry)
                .increment();
    }

    public void recordSmtpError(String cause) {
        Counter.builder("notification_smtp_errors_total")
                .tag("cause", safeTag(cause))
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startSendTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopSendTimer(Timer.Sample sample, String template) {
        sample.stop(Timer.builder("notification_email_send_seconds")
                .tag("template", safeTag(template))
                .register(meterRegistry));
    }

    private static String safeTag(String value) {
        return value == null ? "unknown" : value;
    }
}
