package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request;

import com.bluedigi.bluememo.messaging.domain.enums.IncomingAction;

public record TelegramAction(
        IncomingAction action,
        String message
){}
