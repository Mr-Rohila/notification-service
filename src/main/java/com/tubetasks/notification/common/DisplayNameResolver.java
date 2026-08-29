package com.tubetasks.notification.common;

import org.springframework.util.StringUtils;

public final class DisplayNameResolver {

    private DisplayNameResolver() {}

    public static String resolve(String displayName, String email) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }
        if (StringUtils.hasText(email) && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "there";
    }
}
