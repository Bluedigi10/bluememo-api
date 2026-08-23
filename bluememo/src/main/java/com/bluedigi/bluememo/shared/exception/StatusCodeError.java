package com.bluedigi.bluememo.shared.exception;

public enum StatusCodeError {
    INTERNAL_SERVER_ERROR(500);

    private final int statusCode;
    StatusCodeError(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
