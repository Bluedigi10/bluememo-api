package com.bluedigi.bluememo.messaging.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.infrastructure.persistence.entity.IncomingEventEntity;

@Component
public class IncomingEventEntityMapper {
    public IncomingEventEntity toEntity(IncomingEvent incomingEvent) {
        if (incomingEvent == null) {
            return null;
        }

        return IncomingEventEntity.builder()
                .id(incomingEvent.getId())
                .channelType(incomingEvent.getChannelType())
                .externalEventId(incomingEvent.getExternalEventId())
                .eventType(incomingEvent.getEventType())
                .status(incomingEvent.getStatus())
                .createdAt(incomingEvent.getCreatedAt())
                .processedAt(incomingEvent.getProcessedAt())
                .build();
    }
}
