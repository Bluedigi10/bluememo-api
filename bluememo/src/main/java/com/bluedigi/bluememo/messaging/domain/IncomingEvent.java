package com.bluedigi.bluememo.messaging.domain;

import java.time.Instant;
import java.util.UUID;

import com.bluedigi.bluememo.common.domain.ChannelType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IncomingEvent {

    private UUID id;

    private ChannelType channelType;

    private String externalEventId;

    private String eventType;

    private IncomingEventStatus status;

    private Instant receivedAt;

    private Instant processedAt;
}
