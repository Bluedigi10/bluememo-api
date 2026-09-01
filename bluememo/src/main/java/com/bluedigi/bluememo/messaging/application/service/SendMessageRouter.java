package com.bluedigi.bluememo.messaging.application.service;

import com.bluedigi.bluememo.common.exception.StatusCodeError;
import com.bluedigi.bluememo.messaging.application.exception.MessageChannelNotConfiguredException;
import com.bluedigi.bluememo.messaging.application.port.out.ChannelMessageSender;
import com.bluedigi.bluememo.messaging.application.port.out.SendMessagePort;
import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SendMessageRouter implements SendMessagePort {

    private final Map<ChannelType, ChannelMessageSender> senders;

    public SendMessageRouter(List<ChannelMessageSender> senders) {
        this.senders = senders.stream().collect(
                Collectors.toMap(
                       ChannelMessageSender::supportedChannelType,
                       Function.identity()
                ));
    }

    @Override
    public void send(OutgoingMessage message) {

        ChannelMessageSender sender = senders.get(message.channelType());

        if (sender == null) {
            throw new MessageChannelNotConfiguredException(
                    message.channelType(),
                    StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        sender.send(message);
    }
}
