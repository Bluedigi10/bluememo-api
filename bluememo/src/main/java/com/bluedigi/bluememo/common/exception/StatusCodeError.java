package com.bluedigi.bluememo.common.exception;

public enum StatusCodeError {
    INTERNAL_SERVER_ERROR(500),
    BAD_REQUEST(400);

    private final int statusCode;
    StatusCodeError(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
