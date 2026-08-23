package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.mapper;

import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request.SendMessage;
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
}
