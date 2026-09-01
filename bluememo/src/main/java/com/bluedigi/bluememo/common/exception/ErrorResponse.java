package com.bluedigi.bluememo.common.exception;

import java.time.Instant;

public record ErrorResponse(
    String message,
    int status,
    String path,
    Instant timestamp
) {
}
