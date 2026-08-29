package com.tubetasks.notification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tubetasks.notification.common.NotificationServiceProperties;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class MailComposerTests {

    private MailComposer mailComposer;

    @BeforeEach
    void setUp() {
        NotificationServiceProperties properties = new NotificationServiceProperties();
        properties.getMail().setAllowedActionUrlPrefixes(java.util.List.of("http://localhost:9000"));
        mailComposer = new MailComposer(htmlEngine(), textEngine(), properties);
    }

    @Test
    void rejectsDisallowedActionUrl() {
        assertThatThrownBy(() -> mailComposer.validateActionUrl("https://evil.example/verify"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void composesVerificationMail() {
        TemplateRegistry.TemplateDefinition template = new TemplateRegistry.TemplateDefinition(
                "registration-verification", "Verify your TubeTasks email", "noreply@tubetasks.in", "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template,
                "Jane",
                "jane@example.com",
                "http://localhost:9000/api/v1/auth/register/verify?token=abc",
                Instant.parse("2026-08-30T08:00:00Z"));
        assertThat(composed.htmlBody()).contains("Jane");
        assertThat(composed.textBody()).contains("http://localhost:9000");
    }

    private static SpringTemplateEngine htmlEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static SpringTemplateEngine textEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode(TemplateMode.TEXT);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
