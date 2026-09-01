package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api;

import com.bluedigi.bluememo.channel.telegram.exception.TelegramApiException;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request.SendMessage;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.SendMessageResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.TelegramApiResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.mapper.TelegramSendMessageMapper;
import com.bluedigi.bluememo.channel.telegram.utils.TelegramMessageSplitter;
import com.bluedigi.bluememo.messaging.application.port.out.ChannelMessageSender;
import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TelegramMessageSender implements ChannelMessageSender {
    private final TelegramSendMessageMapper telegramSendMessageMapper;
    private final TelegramApiClient telegramApiClient;
    private final TelegramMessageSplitter splitter;


    @Override
    public ChannelType supportedChannelType() {
        return ChannelType.TELEGRAM;
    }

    @Override
    public void send(OutgoingMessage message) {

        List<String> messages = splitter.split(message.text());

        for (int index = 0; index < messages.size(); index++) {
            String text = messages.get(index);

            try {
                SendMessage request = telegramSendMessageMapper.toSendMessage(message, text);

                TelegramApiResponse<SendMessageResponse> response = telegramApiClient.sendMessage(request);

                validateResponse(response);
            } catch (RuntimeException exception) {
                log.error(
                        "Failed to send Telegram fragment {}/{} for chatId={}",
                        index + 1,
                        messages.size(),
                        message.conversationId(),
                        exception
                );

                throw new TelegramApiException("Failed to send Telegram message", exception);
            }
        }
    }

    private void validateResponse(TelegramApiResponse<SendMessageResponse> response) {
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
