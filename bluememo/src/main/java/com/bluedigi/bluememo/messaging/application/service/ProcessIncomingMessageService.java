package com.bluedigi.bluememo.messaging.application.service;

import com.bluedigi.bluememo.messaging.application.port.out.IncomingEventRepository;
import com.bluedigi.bluememo.messaging.application.port.out.SendMessagePort;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.common.domain.MessageCommands;
import com.bluedigi.bluememo.common.exception.StatusCodeError;
import com.bluedigi.bluememo.identity.application.service.ChannelAccountService;
import com.bluedigi.bluememo.messaging.application.exception.MessageException;
import com.bluedigi.bluememo.messaging.application.mapper.IncomingMessageMapper;
import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;
import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.domain.IncomingEventStatus;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcessIncomingMessageService implements ProcessIncomingMessageUseCase {
    private final SendMessagePort sender;
    private final IncomingEventRepository eventRepository;
    private final IncomingMessageMapper mapper;
    private final ChannelAccountService channelAccountService;

    @Override
    public void process(IncomingMessage message) {

        String reply = processReply(message);

        updateStatus(message, IncomingEventStatus.PROCESSED);

        OutgoingMessage toSend = new OutgoingMessage(
                message.channelType(),
                message.externalMessageId(),
                message.conversationId(),
                reply
        );

        try {
            sender.send(toSend);
            updateStatus(message, IncomingEventStatus.ANSWERED);
        } catch (RuntimeException exception) {
            updateStatus(message, IncomingEventStatus.FAILED);
            throw new MessageException(StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode(), exception);
        }
    }

    private void updateStatus(IncomingMessage message, IncomingEventStatus status) {
        IncomingEvent updateToProcessed = mapper.incomingMessageToIncomingEvent(message, status);
        eventRepository.updateIncomingEventStatus(updateToProcessed);
    }

    private String processReply(IncomingMessage message) {
        String text = message.text();
        if (text == null || text.isEmpty()){
            return "De momento solo proceso texto";
        }

        if (!text.startsWith("/")) {
            return "Recibí " + text;
        }

        log.info("Processing command: {}", text);

        return switch (MessageCommands.fromValue(text)) {
            case START -> linkAccount(message);
            case HELP -> "Estos son los comandos disponibles...";
            default -> "Comando no reconocido";
        };
    }

    private String linkAccount(IncomingMessage message) {

        String externalUserId = message.senderId();
        String externalChatId = message.conversationId();
        String token = extractTokenFromMessage(message.text());
        return channelAccountService.linkAccount(externalUserId, externalChatId, token);
    }

    private String extractTokenFromMessage(String text) {
        if (text == null || !text.startsWith("/start")) {
            return null;
        }
        return text.substring("/start".length()).trim();
    }

}
