package com.tubetasks.notification.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, String> {

    boolean existsByEventId(String eventId);

    java.util.Optional<DeliveryEntity> findFirstByEventId(String eventId);

    @Query(
            """
            select d from DeliveryEntity d
            where (d.callbackStatus = 'PENDING' and d.callbackAttempts < :pendingMax)
               or (d.callbackStatus = 'FAILED' and d.status = 'SENT' and d.callbackAttempts < :failedMax)
            """)
    java.util.List<DeliveryEntity> findCallbacksToRetry(
            @Param("pendingMax") int pendingMax, @Param("failedMax") int failedMax);

    @Modifying
    @Query("delete from DeliveryEntity d where d.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
