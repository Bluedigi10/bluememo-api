package com.bluedigi.bluememo.messaging.domain;

import java.time.Instant;

public record IncomingMessage(
    ChannelType channelType,
    Long externalMessageId,
    Long conversationId,
    Long senderId,
    String text,
    Instant sentDate,
    String username
) {
}