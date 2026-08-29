package com.tubetasks.notification.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {

    Optional<ProcessedEventEntity> findByEventId(String eventId);

    Optional<ProcessedEventEntity> findByBusinessKey(String businessKey);

    @Modifying
    @Query(
            """
            delete from ProcessedEventEntity p
            where p.createdAt < :cutoff
              and p.status in :statuses
            """)
    int deleteCreatedBeforeWithStatuses(
            @Param("cutoff") Instant cutoff, @Param("statuses") Collection<ProcessedEventStatus> statuses);
}
