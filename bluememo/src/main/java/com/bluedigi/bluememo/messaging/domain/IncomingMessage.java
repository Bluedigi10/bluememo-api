package com.bluedigi.bluememo.messaging.domain;

import java.time.Instant;

import com.bluedigi.bluememo.common.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.enums.IncomingAction;

public record IncomingMessage(
    ChannelType channelType,
    String externalMessageId,
    String conversationId,
    String messageId,
    String senderId,
    IncomingAction action,
    String text,
    Instant sentDate,
    String username,
    boolean privateConversation
) {
}
