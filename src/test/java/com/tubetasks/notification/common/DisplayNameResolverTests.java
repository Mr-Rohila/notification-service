package com.tubetasks.notification.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DisplayNameResolverTests {

    @Test
    void usesDisplayNameWhenPresent() {
        assertThat(DisplayNameResolver.resolve(" Jane Doe ", "user@example.com")).isEqualTo("Jane Doe");
    }

    @Test
    void fallsBackToEmailLocalPart() {
        assertThat(DisplayNameResolver.resolve(null, "user@example.com")).isEqualTo("user");
    }

    @Test
    void fallsBackToThereWhenBlank() {
        assertThat(DisplayNameResolver.resolve("", "invalid")).isEqualTo("there");
    }
}
