package com.bluedigi.bluememo.shared.exception;

import org.springframework.http.HttpStatus;

public class BluememoException extends RuntimeException{
    private final Integer statusCode;
    public BluememoException(String message,  Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
