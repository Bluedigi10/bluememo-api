package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdateRequest(
    @JsonProperty("update_id") 
    Long updateId,
    TelegramMessageRequest message
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessageRequest(

            @JsonProperty("message_id")
            Long messageId,

            Long date,

            String text,

            TelegramUserRequest from,

            TelegramChatRequest chat
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramUserRequest(

            Long id,

            @JsonProperty("is_bot")
            Boolean bot,

            @JsonProperty("first_name")
            String firstName,

            String username,

            @JsonProperty("language_code")
            String languageCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramChatRequest(
            Long id,
            String type
    ) {
    }
    
}
