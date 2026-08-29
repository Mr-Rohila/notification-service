package com.tubetasks.notification.common;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

public class SensitiveDataMaskingConverter extends ClassicConverter {

    private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passwd|secret|token|verificationtoken|resettoken|api[-_]?key|"
                    + "private[-_]?key|keystore|truststore|authorization_code|refresh_token|"
                    + "access_token|client_secret|otp|code_verifier|jaas)\\s*([=:])\\s*[^\\s,;]+");
    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*(?:bearer\\s+)?)[^\\s,;]+");
    private static final Pattern TOKEN_QUERY = Pattern.compile(
            "(?i)([?&](?:token|verificationToken|resetToken)=)[^&\\s]+");

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        String masked = CREDENTIAL_ASSIGNMENT.matcher(message).replaceAll("$1$2***");
        masked = AUTHORIZATION_HEADER.matcher(masked).replaceAll("$1***");
        return TOKEN_QUERY.matcher(masked).replaceAll("$1***");
    }
}
