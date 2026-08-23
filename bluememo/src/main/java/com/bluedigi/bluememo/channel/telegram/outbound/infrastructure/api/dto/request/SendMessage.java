package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SendMessage(
        @JsonProperty("chat_id")
        String chatId,
        String text
) {
}
