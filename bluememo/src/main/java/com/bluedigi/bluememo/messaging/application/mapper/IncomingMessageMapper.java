package com.bluedigi.bluememo.messaging.application.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.domain.IncomingEventStatus;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

@Component
public class IncomingMessageMapper {
    public IncomingEvent incomingMessageToIncomingEvent(IncomingMessage message, IncomingEventStatus status) {
        return IncomingEvent.builder()
            .channelType(message.channelType())
            .externalEventId(message.externalMessageId())
            .status(status)
            .processedAt(status == IncomingEventStatus.PROCESSED ? Instant.now() : null)
            .build();
    }
}
