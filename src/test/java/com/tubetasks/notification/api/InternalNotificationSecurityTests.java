package com.tubetasks.notification.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import com.tubetasks.notification.config.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestJwtDecoderConfig.class, TestChannelBinderConfiguration.class})
class InternalNotificationSecurityTests {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(new ServerSetup(3025, "localhost", ServerSetup.PROTOCOL_SMTP));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/internal/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsMissingScope() throws Exception {
        mockMvc.perform(post("/internal/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void acceptsNotificationSendScope() throws Exception {
        mockMvc.perform(post("/internal/api/v1/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_notification.send"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    private static String validBody() {
        return """
                {
                  "eventType": "EMAIL_VERIFICATION_REQUESTED",
                  "payload": {
                    "userId": "user-123",
                    "displayName": "Jane Doe",
                    "email": "jane@example.com",
                    "verificationToken": "token-value",
                    "verificationUrl": "http://localhost:9000/api/v1/auth/register/verify?token=token-value",
                    "tokenExpiresAt": "%s"
                  }
                }
                """
                .formatted(Instant.now().plusSeconds(3600).toString());
    }
}
