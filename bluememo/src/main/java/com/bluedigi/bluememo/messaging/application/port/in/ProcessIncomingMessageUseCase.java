package com.bluedigi.bluememo.messaging.application.port.in;

import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

public interface ProcessIncomingMessageUseCase {
    void process(IncomingMessage message);
}
