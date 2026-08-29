package com.tubetasks.notification;

import com.tubetasks.notification.config.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestJwtDecoderConfig.class, TestChannelBinderConfiguration.class})
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {}
}
