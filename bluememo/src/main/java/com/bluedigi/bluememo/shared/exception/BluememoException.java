package com.bluedigi.bluememo.shared.exception;

import org.springframework.http.HttpStatus;

public class BluememoException extends RuntimeException{
    private final HttpStatus statusCode;
    public BluememoException(String message,  HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }
}
