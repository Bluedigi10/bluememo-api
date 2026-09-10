package com.bluedigi.bluememo.messaging.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.enums.IncomingEventStatus;
import com.bluedigi.bluememo.messaging.infrastructure.persistence.entity.IncomingEventEntity;

public interface IncomingEventJpaRepository extends JpaRepository<IncomingEventEntity, UUID> {
    // Additional query methods can be defined here if needed
    @Modifying
    @Query(value = """
        INSERT INTO incoming_events (
            id,
            channel_type,
            external_event_id,
            event_type,
            status,
            received_at,
            processed_at
        )
        VALUES (
            :#{#event.id},
            :#{#event.channelType.name()},
            :#{#event.externalEventId},
            :#{#event.eventType},
            :#{#event.status.name()},
            :#{#event.receivedAt},
            :#{#event.processedAt}
        )
        ON CONFLICT (channel_type, external_event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("event") IncomingEventEntity event
    );

    @Modifying
    @Query(value = """
            UPDATE incoming_events
            SET status = :#{#status.name()},
                processed_at = COALESCE(:processedAt, processed_at)
            WHERE channel_type = :#{#channelType.name()}
            AND external_event_id = :externalEventId
            """, nativeQuery = true)
    int updateStatusByExternalEventIdAndChannelType(
            @Param("externalEventId") String externalEventId,
            @Param("channelType") ChannelType channelType,
            @Param("status") IncomingEventStatus status,
            @Param("processedAt") Instant processedAt
    );
}
