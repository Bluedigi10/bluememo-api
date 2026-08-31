package com.bluedigi.bluememo.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,
    int status,
    String path,
    LocalDateTime timestamp
) {
}