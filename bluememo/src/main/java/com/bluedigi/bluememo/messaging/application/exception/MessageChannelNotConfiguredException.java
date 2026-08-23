package com.bluedigi.bluememo.messaging.application.exception;

import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.shared.exception.BluememoException;
import org.springframework.http.HttpStatus;

public class MessageChannelNotConfiguredException extends BluememoException {

    public MessageChannelNotConfiguredException(ChannelType channel, Integer statusCode) {
        super("Channel " + channel + " is not supported", statusCode);
    }
}
