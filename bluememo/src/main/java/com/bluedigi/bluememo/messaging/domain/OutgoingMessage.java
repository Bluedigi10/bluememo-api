package com.bluedigi.bluememo.messaging.domain;

public record OutgoingMessage(
    ChannelType channelType,
    String externalMessageId,
    String conversationId,
    String text
) {
}
