package com.bluedigi.bluememo.messaging.application.exception;

import com.bluedigi.bluememo.common.exception.BluememoException;
import com.bluedigi.bluememo.messaging.domain.ChannelType;

public class MessageChannelNotConfiguredException extends BluememoException {

    public MessageChannelNotConfiguredException(ChannelType channel, Integer statusCode) {
        super("Channel: " + channel + " is not supported", statusCode);
    }
}
