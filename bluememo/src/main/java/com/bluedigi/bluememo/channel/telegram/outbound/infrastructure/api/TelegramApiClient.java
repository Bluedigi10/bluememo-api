package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api;

import com.bluedigi.bluememo.channel.telegram.exception.TelegramApiException;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.TelegramApiPaths;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request.SendMessage;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.SendMessageResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.TelegramApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramApiClient {
    private final RestClient telegramRestClient;

    public TelegramApiClient(
            @Qualifier("telegramRestClient")
            RestClient telegramRestClient) {
        this.telegramRestClient = telegramRestClient;
    }

    public TelegramApiResponse<SendMessageResponse> sendMessage(SendMessage message) {

        return telegramRestClient.post()
                .uri(TelegramApiPaths.SEND_MESSAGE.getPath())
                .body(message)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            throw new TelegramApiException(
                                    "Telegram API returned status " + response.getStatusCode()
                            );
                        }
                )
                .body(new ParameterizedTypeReference<>() {
                });
    }

}
