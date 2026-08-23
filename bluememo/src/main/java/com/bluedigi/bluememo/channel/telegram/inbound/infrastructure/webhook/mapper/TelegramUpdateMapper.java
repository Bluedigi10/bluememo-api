package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.mapper;

import com.bluedigi.bluememo.messaging.domain.ChannelType;
import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramUpdateRequest;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

import java.time.Instant;

@Component
public class TelegramUpdateMapper {
    public IncomingMessage mapToIncomingMessage(TelegramUpdateRequest request){
        return new IncomingMessage(
                ChannelType.TELEGRAM,
                String.valueOf(request.updateId()),
                String.valueOf(request.message().chat().id()),
                String.valueOf(request.message().messageId()),
                String.valueOf(request.message().from().id()),
                validateMessageTextNull(request),
                toDate(request.message().date()),
                request.message().from().username()
        );
    }
    private String validateMessageTextNull(TelegramUpdateRequest request){
        return request.message().text() == null ? null : request.message().text();
    }

    private Instant toDate(Long date){
        return Instant.ofEpochSecond(date);
    }
}
