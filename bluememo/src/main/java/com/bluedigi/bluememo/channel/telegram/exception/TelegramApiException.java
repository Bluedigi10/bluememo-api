package com.bluedigi.bluememo.channel.telegram.exception;

import com.bluedigi.bluememo.shared.exception.BluememoException;
import org.springframework.http.HttpStatus;

public class TelegramApiException extends BluememoException {
    public TelegramApiException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
