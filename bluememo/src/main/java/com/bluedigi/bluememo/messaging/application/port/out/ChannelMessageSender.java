package com.bluedigi.bluememo.messaging.application.port.out;

import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

public interface ChannelMessageSender {
    ChannelType supportedChannelType();
    void send(OutgoingMessage message);
}
