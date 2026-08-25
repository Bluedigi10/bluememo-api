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

        IncomingMessage message = telegramUpdateMapper.mapToIncomingMessage(request);
        processIncomingMessageUseCase.process(message);
    }
}
