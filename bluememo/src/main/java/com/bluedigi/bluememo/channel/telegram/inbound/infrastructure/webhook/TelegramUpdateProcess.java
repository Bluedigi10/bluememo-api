package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.mapper.TelegramUpdateMapper;
import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramUpdateRequest;
import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcess {
    private final ProcessIncomingMessageUseCase processIncomingMessageUseCase;
    private final TelegramUpdateMapper telegramUpdateMapper;

    public void process(TelegramUpdateRequest request) {
        if (request.message() == null) {
            return;
        }

        if (!hasRequiredFields(request)) {
            return;
        }

        IncomingMessage message = telegramUpdateMapper.mapToIncomingMessage(request);
        processIncomingMessageUseCase.process(message);
    }

    private boolean hasRequiredFields(TelegramUpdateRequest update) {
        TelegramUpdateRequest.TelegramMessageRequest message =
                update.message();

        return update.updateId() != null
                && message.messageId() != null
                && message.date() != null
                && message.chat() != null
                && message.chat().id() != null
                && message.from() != null
                && message.from().id() != null;
    }
}
