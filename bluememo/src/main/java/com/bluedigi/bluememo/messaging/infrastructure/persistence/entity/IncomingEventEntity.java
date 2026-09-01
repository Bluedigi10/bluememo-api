package com.bluedigi.bluememo.messaging.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.IncomingEventStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@Table(name = "incoming_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class IncomingEventEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, updatable = false)
    private ChannelType channelType;

    @Column(name = "external_event_id", nullable = false, updatable = false)
    private String externalEventId;

    @Column(name = "event_type", updatable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomingEventStatus status;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
