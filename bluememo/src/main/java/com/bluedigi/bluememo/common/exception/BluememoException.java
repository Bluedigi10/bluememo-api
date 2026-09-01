package com.bluedigi.bluememo.common.exception;

public class BluememoException extends RuntimeException{
    private final Integer statusCode;
    public BluememoException(String message,  Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public BluememoException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }


    public Integer getStatusCode() {
        return statusCode;
    }
}
