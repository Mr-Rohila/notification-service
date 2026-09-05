package com.tubetasks.notification.job;

import com.tubetasks.notification.common.NotificationServiceProperties;
import com.tubetasks.notification.persistence.DeliveryRepository;
import com.tubetasks.notification.persistence.ProcessedEventRepository;
import com.tubetasks.notification.persistence.ProcessedEventStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetentionJob.class);

    private final NotificationServiceProperties properties;
    private final DeliveryRepository deliveryRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationRetentionJob(
            NotificationServiceProperties properties,
            DeliveryRepository deliveryRepository,
            ProcessedEventRepository processedEventRepository) {
        this.properties = properties;
        this.deliveryRepository = deliveryRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Scheduled(cron = "${notification-service.retention.cleanup-cron}")
    @Transactional
    public void cleanup() {
        Instant deliveryCutoff =
                Instant.now().minus(properties.getRetention().getDeliveryDays(), ChronoUnit.DAYS);
        Instant processedCutoff =
                Instant.now().minus(properties.getRetention().getProcessedEventDays(), ChronoUnit.DAYS);
        int deliveriesRemoved = deliveryRepository.deleteCreatedBefore(deliveryCutoff);
        int processedRemoved = processedEventRepository.deleteCreatedBeforeWithStatuses(
                processedCutoff,
                EnumSet.of(
                        ProcessedEventStatus.SENT,
                        ProcessedEventStatus.SKIPPED,
                        ProcessedEventStatus.EXPIRED,
                        ProcessedEventStatus.FAILED));
        log.info(
                "Retention cleanup removed deliveries={} processedEvents={}",
                deliveriesRemoved,
                processedRemoved);
    }
}
