package com.tubetasks.notification.common;

import java.time.Duration;
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
    private UserService userService = new UserService();
    private Duration callbackRetryDelay = Duration.ofSeconds(30);

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
        private String appHomeUrl;

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

        public String getAppHomeUrl() {
            return appHomeUrl;
        }

        public void setAppHomeUrl(String appHomeUrl) {
            this.appHomeUrl = appHomeUrl;
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

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public Duration getCallbackRetryDelay() {
        return callbackRetryDelay;
    }

    public void setCallbackRetryDelay(Duration callbackRetryDelay) {
        this.callbackRetryDelay = callbackRetryDelay;
    }

    public static class UserService {
        private String baseUrl = "lb://user-service/user";
        private String deliveryPath = "/internal/api/v1/broadcasts/delivery";
        private String tokenUrl = "lb://auth-server/auth/oauth2/token";
        private String clientId = "notification-service";
        private String clientSecret = "";
        private String scope = "internal.call";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getDeliveryPath() {
            return deliveryPath;
        }

        public void setDeliveryPath(String deliveryPath) {
            this.deliveryPath = deliveryPath;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
