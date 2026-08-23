package com.bluedigi.bluememo.messaging.domain;

import java.time.Instant;

public record IncomingMessage(
    ChannelType channelType,
    String externalMessageId,
    String conversationId,
    String messageId,
    String senderId,
    String text,
    Instant sentDate,
    String username
) {
}