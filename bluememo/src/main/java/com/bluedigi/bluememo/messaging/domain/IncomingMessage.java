package com.bluedigi.bluememo.messaging.domain;

import java.time.Instant;

import com.bluedigi.bluememo.common.domain.ChannelType;

public record IncomingMessage(
    ChannelType channelType,
    String externalMessageId,
    String conversationId,
    String messageId,
    String senderId,
    String text,
    Instant sentDate,
    String username,
    boolean privateConversation
) {
}
