package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api;

import com.bluedigi.bluememo.channel.telegram.exception.TelegramApiException;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request.SendMessage;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.SendMessageResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.TelegramApiResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.mapper.TelegramSendMessageMapper;
import com.bluedigi.bluememo.messaging.application.port.out.ChannelMessageSender;
import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TelegramMessageSender implements ChannelMessageSender {
    private final TelegramSendMessageMapper telegramSendMessageMapper;
    private final TelegramApiClient telegramApiClient;

    public TelegramMessageSender(TelegramSendMessageMapper telegramSendMessageMapper, TelegramApiClient telegramApiClient) {
        this.telegramSendMessageMapper = telegramSendMessageMapper;
        this.telegramApiClient = telegramApiClient;
    }


    @Override
    public ChannelType supportedChannelType() {
        return ChannelType.TELEGRAM;
    }

    @Override
    public void send(OutgoingMessage message) {
        SendMessage request = telegramSendMessageMapper.toSendMessage(message);
        TelegramApiResponse<SendMessageResponse> response = telegramApiClient.sendMessage(request);
        if (response == null || !response.ok()) {
            String messageError = response != null && response.description() != null
                    ? response.description()
                    : "Telegram API returned an invalid response";

            throw new TelegramApiException(
                    messageError
            );
        }

        log.info(
                "Message identifier: {}:{}:{}",
                ChannelType.TELEGRAM.name(),
                response.result().chat().id(),
                response.result().messageId()
        );
    }
}
