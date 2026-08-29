package com.tubetasks.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationDispatchServiceBusinessKeyTests {

    @Test
    void computesDeterministicBusinessKey() {
        String first = NotificationDispatchService.computeBusinessKey(
                "EMAIL_VERIFICATION_REQUESTED", "user-1", "token-abc");
        String second = NotificationDispatchService.computeBusinessKey(
                "EMAIL_VERIFICATION_REQUESTED", "user-1", "token-abc");
        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    void changesWhenTokenChanges() {
        String first = NotificationDispatchService.computeBusinessKey(
                "EMAIL_VERIFICATION_REQUESTED", "user-1", "token-a");
        String second = NotificationDispatchService.computeBusinessKey(
                "EMAIL_VERIFICATION_REQUESTED", "user-1", "token-b");
        assertThat(first).isNotEqualTo(second);
    }
}
