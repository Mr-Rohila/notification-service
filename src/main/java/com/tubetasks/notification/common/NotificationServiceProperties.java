package com.tubetasks.notification.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification-service")
public class NotificationServiceProperties {

    private Mail mail = new Mail();
    private Map<String, TemplateDefinition> templates = new LinkedHashMap<>();
    private Retention retention = new Retention();
    private boolean consumerEnabled = true;
    private boolean sendEnabled = false;

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Map<String, TemplateDefinition> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, TemplateDefinition> templates) {
        this.templates = templates;
    }

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    public boolean isConsumerEnabled() {
        return consumerEnabled;
    }

    public void setConsumerEnabled(boolean consumerEnabled) {
        this.consumerEnabled = consumerEnabled;
    }

    public boolean isSendEnabled() {
        return sendEnabled;
    }

    public void setSendEnabled(boolean sendEnabled) {
        this.sendEnabled = sendEnabled;
    }

    public static class Mail {
        private String from;
        private String fromName;
        private List<String> allowedActionUrlPrefixes = new ArrayList<>();
        private String supportEmail;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public List<String> getAllowedActionUrlPrefixes() {
            return allowedActionUrlPrefixes;
        }

        public void setAllowedActionUrlPrefixes(List<String> allowedActionUrlPrefixes) {
            this.allowedActionUrlPrefixes = allowedActionUrlPrefixes;
        }

        public String getSupportEmail() {
            return supportEmail;
        }

        public void setSupportEmail(String supportEmail) {
            this.supportEmail = supportEmail;
        }
    }

    public static class TemplateDefinition {
        private String name;
        private String subject;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }

    public static class Retention {
        private int processedEventDays;
        private int deliveryDays;
        private String cleanupCron;

        public int getProcessedEventDays() {
            return processedEventDays;
        }

        public void setProcessedEventDays(int processedEventDays) {
            this.processedEventDays = processedEventDays;
        }

        public int getDeliveryDays() {
            return deliveryDays;
        }

        public void setDeliveryDays(int deliveryDays) {
            this.deliveryDays = deliveryDays;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }
    }
}
