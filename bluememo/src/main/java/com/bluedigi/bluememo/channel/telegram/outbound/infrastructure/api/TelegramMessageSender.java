package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api;

import com.bluedigi.bluememo.channel.telegram.exception.TelegramApiException;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.request.SendMessage;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.SendMessageResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto.response.TelegramApiResponse;
import com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.mapper.TelegramSendMessageMapper;
import com.bluedigi.bluememo.messaging.application.port.out.ChannelMessageSender;
import com.bluedigi.bluememo.messaging.application.port.out.IncomingEventRepository;
import com.bluedigi.bluememo.messaging.domain.ChannelType;
import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.domain.IncomingEventStatus;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TelegramMessageSender implements ChannelMessageSender {
    private final TelegramSendMessageMapper telegramSendMessageMapper;
    private final TelegramApiClient telegramApiClient;
    private final IncomingEventRepository eventRepository;


    @Override
    public ChannelType supportedChannelType() {
        return ChannelType.TELEGRAM;
    }

    @Override
    public void send(OutgoingMessage message) {

        List<String> messages = messageLongValidator(message.text());

        messages.forEach(text -> {
            SendMessage request = telegramSendMessageMapper.toSendMessage(message, text);
        
            TelegramApiResponse<SendMessageResponse> response = telegramApiClient.sendMessage(request);
            
            validateResponse(response);
        });

        IncomingEvent messageAnswered = telegramSendMessageMapper.toIncomingEvent(message.externalMessageId(), IncomingEventStatus.ANSWERED);

        eventRepository.updateIncomingEventStatus(messageAnswered);
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

    private List<String> messageLongValidator(String text) {
        Integer maxLength = 4095;
        if (text == null || text.isEmpty()) {
            return List.of(text);
        }

        String[] words = text.split("\\s+");
        List<String> finalMessages = new ArrayList<>();
        StringBuilder actualText = new StringBuilder();

        for(String word: words) {

            if (!actualText.isEmpty() && (actualText.length() + word.length() > maxLength)) {
                finalMessages.add(actualText.toString());
                actualText = new StringBuilder();
            }
            
            actualText.append(word);
            actualText.append(" ");
            
        }

        if (!actualText.isEmpty()) {
            finalMessages.add(actualText.toString());
        }

        return finalMessages;
        
    }
}
