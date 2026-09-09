package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.mapper;

import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.domain.IncomingEventStatus;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramUpdateRequest;
import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

import java.time.Instant;
import java.util.UUID;

@Component
public class TelegramUpdateMapper {
    public IncomingMessage mapToIncomingMessage(TelegramUpdateRequest request){
        return new IncomingMessage(
                ChannelType.TELEGRAM,
                request.updateId().toString(),
                request.message().chat().id().toString(),
                request.message().messageId().toString(),
                request.message().from().id().toString(),
                request.message().text(),
                toDate(request.message().date()),
                request.message().from().username(),
                "private".equals(request.message().chat().type())
        );
    }

    private Instant toDate(Long date){
        return Instant.ofEpochSecond(date);
    }

    public IncomingEvent mapToIncomingEvent(TelegramUpdateRequest request, IncomingEventStatus status) {
        return IncomingEvent.builder()
                .id(UUID.randomUUID())
                .channelType(ChannelType.TELEGRAM)
                .externalEventId(request.updateId().toString())
                .eventType("message")
                .status(status)
                .receivedAt(Instant.now())
                .build();
    }
}
