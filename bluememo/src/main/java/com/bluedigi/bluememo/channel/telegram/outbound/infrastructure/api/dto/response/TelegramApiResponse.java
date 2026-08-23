package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramApiResponse<T>(
        boolean ok,
        T result,
        String description,

        @JsonProperty("error_code")
        Integer errorCode,

        TelegramResponseParameters parameters
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramResponseParameters(

            @JsonProperty("migrate_to_chat_id")
            Long migrateToChatId,

            @JsonProperty("retry_after")
            Integer retryAfter
    ) {
    }
}
