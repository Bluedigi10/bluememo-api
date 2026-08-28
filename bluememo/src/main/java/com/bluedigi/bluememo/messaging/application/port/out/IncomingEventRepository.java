package com.bluedigi.bluememo.messaging.application.port.out;

import com.bluedigi.bluememo.messaging.domain.IncomingEvent;

public interface IncomingEventRepository {
    int insertIfAbsent(IncomingEvent incomingEvent);
}
