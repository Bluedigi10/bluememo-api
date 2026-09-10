package com.bluedigi.bluememo.identity.infrastructure.web.response;

import java.time.Instant;

public record LinkChannelResponse(
    String linkUrl,
    Instant expirationDate
) {
}
