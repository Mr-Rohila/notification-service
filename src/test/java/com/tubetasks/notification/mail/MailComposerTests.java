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
        properties.getMail().setAllowedActionUrlPrefixes(
                java.util.List.of("http://localhost:9000/auth", "http://localhost:4200"));
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
                "http://localhost:9000/auth/api/v1/auth/register/verify?token=abc",
                Instant.parse("2026-08-30T08:00:00Z"));
        assertThat(composed.htmlBody()).contains("Jane");
        assertThat(composed.htmlBody()).contains("Email Verification");
        assertThat(composed.htmlBody()).contains("Verify Email");
        assertThat(composed.htmlBody()).contains("This is an automated message");
        assertThat(composed.textBody()).contains("http://localhost:9000/auth");
        assertThat(composed.textBody()).contains("TubeTasks");
    }

    @Test
    void composesWelcomeMail() {
        TemplateRegistry.TemplateDefinition template =
                new TemplateRegistry.TemplateDefinition("welcome", "Welcome to TubeTasks", "noreply@tubetasks.in", "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template, "Jane", "jane@example.com", "http://localhost:9000/auth/login", null);
        assertThat(composed.htmlBody()).contains("Welcome to TubeTasks");
        assertThat(composed.htmlBody()).contains("Log in");
        assertThat(composed.textBody()).contains("http://localhost:9000/auth/login");
    }

    @Test
    void composesPaymentSubmittedMail() {
        TemplateRegistry.TemplateDefinition template = new TemplateRegistry.TemplateDefinition(
                "payment-submitted", "We received your TubeTasks deposit request", "noreply@tubetasks.in", "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template,
                "Jane",
                "jane@example.com",
                null,
                null,
                java.util.Map.of("amount", "100.0000", "currency", "INR", "transactionId", "txn-1"));
        assertThat(composed.htmlBody()).contains("Deposit request received");
        assertThat(composed.htmlBody()).contains("100.0000");
        assertThat(composed.textBody()).contains("txn-1");
    }

    @Test
    void composesAdminWalletCreditedMail() {
        TemplateRegistry.TemplateDefinition template = new TemplateRegistry.TemplateDefinition(
                "admin-wallet-credited", "Wallet credited", "noreply@tubetasks.in", "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template,
                "Jane",
                "jane@example.com",
                null,
                null,
                java.util.Map.of("amount", "500.0000", "currency", "INR"));
        assertThat(composed.htmlBody()).contains("Wallet credited");
        assertThat(composed.htmlBody()).contains("500.0000");
        assertThat(composed.htmlBody()).contains("Added by TubeTasks support");
        assertThat(composed.textBody()).contains("500.0000");
        assertThat(composed.textBody()).contains("Added by TubeTasks support");
    }

    @Test
    void composesSubscriptionPurchasedMail() {
        TemplateRegistry.TemplateDefinition template = new TemplateRegistry.TemplateDefinition(
                "subscription-purchased",
                "Your TubeTasks campaign is active",
                "noreply@tubetasks.in",
                "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template,
                "Jane",
                "jane@example.com",
                null,
                null,
                java.util.Map.of(
                        "purchaseId",
                        "purchase-1",
                        "planTitle",
                        "Starter",
                        "channelTitle",
                        "My Channel",
                        "amount",
                        "199.0000",
                        "currency",
                        "INR"));
        assertThat(composed.htmlBody()).contains("Your campaign is active");
        assertThat(composed.htmlBody()).contains("Starter");
        assertThat(composed.textBody()).contains("purchase-1");
    }

    @Test
    void composesCampaignCompletedMail() {
        TemplateRegistry.TemplateDefinition template = new TemplateRegistry.TemplateDefinition(
                "campaign-completed",
                "Your TubeTasks campaign is complete",
                "noreply@tubetasks.in",
                "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template,
                "Jane",
                "jane@example.com",
                null,
                null,
                java.util.Map.of(
                        "purchaseId",
                        "purchase-1",
                        "planTitle",
                        "Starter",
                        "channelTitle",
                        "My Channel",
                        "amount",
                        "199.0000",
                        "currency",
                        "INR"));
        assertThat(composed.htmlBody()).contains("Your campaign is complete");
        assertThat(composed.textBody()).contains("subscriber target");
    }

    @Test
    void composesTaskAssignedMail() {
        TemplateRegistry.TemplateDefinition template = new TemplateRegistry.TemplateDefinition(
                "task-assigned",
                "You have a new TubeTasks task",
                "noreply@tubetasks.in",
                "TubeTasks");
        MailComposer.ComposedMail composed = mailComposer.compose(
                template,
                "Jane",
                "jane@example.com",
                "http://localhost:4200",
                null,
                java.util.Map.of(
                        "planTitle",
                        "Starter",
                        "channelTitle",
                        "My Channel",
                        "channelUrl",
                        "https://youtube.com/@mychannel",
                        "amount",
                        "5.0000",
                        "currency",
                        "INR"));
        assertThat(composed.htmlBody()).contains("You have a new task");
        assertThat(composed.htmlBody()).contains("5.0000");
        assertThat(composed.htmlBody()).contains("Open TubeTasks");
        assertThat(composed.htmlBody()).contains("http://localhost:4200");
        assertThat(composed.textBody()).contains("assigned to you");
        assertThat(composed.textBody()).contains("http://localhost:4200");
        assertThat(composed.htmlBody()).doesNotContain("My Channel", "https://youtube.com/@mychannel", "Starter");
        assertThat(composed.textBody()).doesNotContain("My Channel", "https://youtube.com/@mychannel", "Starter");
    }

    @Test
    void allowsMissingActionUrlForInformationalMail() {
        mailComposer.validateActionUrl(null);
        mailComposer.validateActionUrl("");
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
