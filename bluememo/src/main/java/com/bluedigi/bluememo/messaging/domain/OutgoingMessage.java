package com.bluedigi.bluememo.messaging.domain;

import com.bluedigi.bluememo.common.domain.ChannelType;

public record OutgoingMessage(
    ChannelType channelType,
    String externalMessageId,
    String conversationId,
    String text
) {
}
