package com.bluedigi.bluememo.messaging.application.port.out;

import com.bluedigi.bluememo.messaging.domain.IncomingEvent;

public interface IncomingEventRepository {
    Boolean insertIfAbsent(IncomingEvent incomingEvent);
    Boolean updateIncomingEventStatus(IncomingEvent incomingEvent);
}
