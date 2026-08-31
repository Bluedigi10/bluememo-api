package com.bluedigi.bluememo.channel.telegram.exception;

import com.bluedigi.bluememo.common.exception.BluememoException;
import com.bluedigi.bluememo.common.exception.StatusCodeError;

public class TelegramApiException extends BluememoException {
    public TelegramApiException(String message) {
        super(message, StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
