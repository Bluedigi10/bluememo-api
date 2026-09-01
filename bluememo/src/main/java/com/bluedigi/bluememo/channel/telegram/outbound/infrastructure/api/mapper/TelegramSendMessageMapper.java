package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.mapper;

import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request.SendMessage;
import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.domain.IncomingEventStatus;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;
import org.springframework.stereotype.Component;

@Component
public class TelegramSendMessageMapper {
    public SendMessage toSendMessage(OutgoingMessage message) {
        return new SendMessage(
                message.conversationId(),
                message.text()
        );
    }

    public SendMessage toSendMessage(OutgoingMessage message, String text) {
        return new SendMessage(
                message.conversationId(),
                text
        );
    }

    public IncomingEvent toIncomingEvent(String externalMessageId, IncomingEventStatus status) {

        return IncomingEvent.builder()
            .channelType(ChannelType.TELEGRAM)
            .externalEventId(externalMessageId)
            .status(status)
            .build();
    }
}
