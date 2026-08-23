package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SendMessageResponse(
        @JsonProperty("message_id")
        Integer messageId,

        Integer date,

        TelegramChatResponse chat,

        String text
) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TelegramChatResponse(
                Long id,
                String type
        ) {
        }
}
