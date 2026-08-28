package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.mapper.TelegramUpdateMapper;
import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramUpdateRequest;
import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;
import com.bluedigi.bluememo.messaging.application.port.out.IncomingEventRepository;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateProcess {
    private final ProcessIncomingMessageUseCase processIncomingMessageUseCase;
    private final TelegramUpdateMapper telegramUpdateMapper;
    private final IncomingEventRepository incomingEventRepository;

    public void process(TelegramUpdateRequest request) {
        if (request.message() == null) {
            log.debug("TelegramUpdateProcess::process: message is null");
            return;
        }

        if (!hasRequiredFields(request)) {
            log.debug("TelegramUpdateProcess::process: message fields are required");
            return;
        }

        Integer isInserted = incomingEventRepository.insertIfAbsent(telegramUpdateMapper.mapToIncomingEvent(request));

        if (isInserted == 0) {
            log.debug("TelegramUpdateProcess::process: event already exists, skipping processing");
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
