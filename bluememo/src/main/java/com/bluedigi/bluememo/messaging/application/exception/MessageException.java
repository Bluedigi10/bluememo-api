package com.bluedigi.bluememo.messaging.application.exception;

import com.bluedigi.bluememo.common.exception.BluememoException;

public class MessageException extends BluememoException {

    public MessageException(Integer statusCode) {
        super("Error al procesar/enviar el mensaje", statusCode);
    }

    public MessageException(Integer statusCode, Throwable cause) {
        super(
                "Error al procesar/enviar el mensaje",
                statusCode,
                cause
        );
    }
}