package com.tubetasks.notification.mail;

import com.tubetasks.notification.common.NotificationServiceProperties;
import com.tubetasks.notification.event.NotificationEventType;
import org.springframework.stereotype.Component;

@Component
public class TemplateRegistry {

    private final NotificationServiceProperties properties;

    public TemplateRegistry(NotificationServiceProperties properties) {
        this.properties = properties;
    }

    public TemplateDefinition resolve(String eventType) {
        NotificationServiceProperties.TemplateDefinition configured =
                properties.getTemplates().get(eventType);
        if (configured == null) {
            throw new IllegalArgumentException("No template configured for eventType: " + eventType);
        }
        return new TemplateDefinition(
                configured.getName(),
                configured.getSubject(),
                properties.getMail().getFrom(),
                properties.getMail().getFromName());
    }

    public boolean isKnownEventType(String eventType) {
        return NotificationEventType.isKnown(eventType);
    }

    public record TemplateDefinition(String templateName, String subject, String fromEmail, String fromName) {}
}
