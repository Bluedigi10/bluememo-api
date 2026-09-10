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

        String[] parts = extractCommandFromMessage(text);
        String command = parts[0];
        String token = parts.length > 1 ? parts[1] : null;

        log.info("Processing command: {}", MessageCommands.fromValue(command));

        return switch (MessageCommands.fromValue(command)) {
            case START -> processStart(message, token);
            case CHECK_LINK -> checkLink(message);
            case HELP -> "Estos son los comandos disponibles...";
            default -> "Comando no reconocido";
        };
    }

    private String processStart(IncomingMessage message, String token) {
        if (token == null || token.isBlank()) {
            return "Bienvenido, soy un bot";
        }

        if (!message.privateConversation()) {
            return "La vinculación solo está disponible en una conversación privada";
        }

        String externalUserId = message.senderId();
        String externalChatId = message.conversationId();
        return channelAccountService.linkAccount(externalUserId, message.channelType(), externalChatId, token);
    }

    private String checkLink(IncomingMessage message) {
        if (!message.privateConversation()) {
            return "Este comando solo está disponible en una conversación privada";
        }

        boolean linked = channelAccountService.isLinked(
                message.channelType(),
                message.senderId(),
                message.conversationId());

        return linked
                ? "Tu cuenta de Telegram está vinculada a BlueMemo."
                : "Tu cuenta de Telegram no está vinculada a BlueMemo.";
    }

    private String[] extractCommandFromMessage(String text) {
        return text.trim().split("\\s+", 2);
    }

}
