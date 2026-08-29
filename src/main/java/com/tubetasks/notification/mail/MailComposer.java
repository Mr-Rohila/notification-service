package com.tubetasks.notification.mail;

import com.tubetasks.notification.common.NotificationServiceProperties;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class MailComposer {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final SpringTemplateEngine htmlTemplateEngine;
    private final SpringTemplateEngine textTemplateEngine;
    private final NotificationServiceProperties properties;

    public MailComposer(
            @Qualifier("htmlTemplateEngine") SpringTemplateEngine htmlTemplateEngine,
            @Qualifier("textTemplateEngine") SpringTemplateEngine textTemplateEngine,
            NotificationServiceProperties properties) {
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.textTemplateEngine = textTemplateEngine;
        this.properties = properties;
    }

    public ComposedMail compose(
            TemplateRegistry.TemplateDefinition template,
            String displayName,
            String email,
            String actionUrl,
            Instant tokenExpiresAt) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("displayName", displayName);
        variables.put("email", email);
        variables.put("actionUrl", actionUrl);
        variables.put("tokenExpiresAt", EXPIRY_FORMAT.format(tokenExpiresAt));
        String supportEmail = properties.getMail().getSupportEmail();
        if (StringUtils.hasText(supportEmail)) {
            variables.put("supportEmail", supportEmail);
        }
        Context context = new Context();
        context.setVariables(variables);
        String html = htmlTemplateEngine.process("mail/" + template.templateName(), context);
        String text = textTemplateEngine.process("mail/" + template.templateName(), context);
        return new ComposedMail(template, email, html, text);
    }

    public void validateActionUrl(String actionUrl) {
        if (!StringUtils.hasText(actionUrl)) {
            throw new IllegalArgumentException("Action URL is required");
        }
        if (!actionUrl.startsWith("http://") && !actionUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Action URL must use http or https");
        }
        List<String> prefixes = properties.getMail().getAllowedActionUrlPrefixes();
        if (prefixes == null || prefixes.isEmpty()) {
            return;
        }
        boolean allowed = prefixes.stream().anyMatch(actionUrl::startsWith);
        if (!allowed) {
            throw new IllegalArgumentException("Action URL is not allowed");
        }
    }

    public record ComposedMail(
            TemplateRegistry.TemplateDefinition template, String recipientEmail, String htmlBody, String textBody) {}
}
