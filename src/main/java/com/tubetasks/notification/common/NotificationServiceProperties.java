package com.tubetasks.notification.common;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification-service")
public class NotificationServiceProperties {

    private Mail mail = new Mail();
    private Map<String, TemplateDefinition> templates = defaultTemplates();
    private Retention retention = new Retention();
    private boolean consumerEnabled = true;

    private static Map<String, TemplateDefinition> defaultTemplates() {
        Map<String, TemplateDefinition> defaults = new LinkedHashMap<>();
        TemplateDefinition verification = new TemplateDefinition();
        verification.setName("registration-verification");
        verification.setSubject("Verify your TubeTasks email");
        defaults.put("EMAIL_VERIFICATION_REQUESTED", verification);
        TemplateDefinition reset = new TemplateDefinition();
        reset.setName("password-reset");
        reset.setSubject("Reset your TubeTasks password");
        defaults.put("PASSWORD_RESET_REQUESTED", reset);
        return defaults;
    }

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

    public static class Mail {
        private String from = "noreply@tubetasks.in";
        private String fromName = "TubeTasks";
        private List<String> allowedActionUrlPrefixes = List.of("http://localhost:9000");
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
        private int processedEventDays = 30;
        private int deliveryDays = 90;
        private String cleanupCron = "0 30 3 * * *";

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
