package com.bluedigi.bluememo.messaging.domain;

import java.time.LocalDateTime;
import java.util.UUID;

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
    
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
