package com.tubetasks.notification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tubetasks.notification.common.NotificationServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateRegistryTests {

    private TemplateRegistry templateRegistry;

    @BeforeEach
    void setUp() {
        templateRegistry = new TemplateRegistry(new NotificationServiceProperties());
    }

    @Test
    void resolvesVerificationTemplate() {
        TemplateRegistry.TemplateDefinition definition =
                templateRegistry.resolve("EMAIL_VERIFICATION_REQUESTED");
        assertThat(definition.templateName()).isEqualTo("registration-verification");
        assertThat(definition.subject()).isEqualTo("Verify your TubeTasks email");
        assertThat(definition.fromEmail()).isEqualTo("noreply@tubetasks.in");
    }

    @Test
    void rejectsUnknownEventType() {
        assertThat(templateRegistry.isKnownEventType("UNKNOWN")).isFalse();
        assertThatThrownBy(() -> templateRegistry.resolve("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
