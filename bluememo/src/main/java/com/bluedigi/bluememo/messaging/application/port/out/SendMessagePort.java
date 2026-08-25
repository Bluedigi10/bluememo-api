package com.bluedigi.bluememo.messaging.application.port.out;

import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

public interface SendMessagePort {
    void send(OutgoingMessage message);
}
