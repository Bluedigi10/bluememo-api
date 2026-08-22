package com.bluedigi.bluememo.messaging.domain;

public record OutgoingMessage(
    ChannelType channelType,
    String conversationId,
    String text
) {
}
