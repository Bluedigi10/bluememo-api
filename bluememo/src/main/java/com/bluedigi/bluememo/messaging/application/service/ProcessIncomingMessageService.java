package com.bluedigi.bluememo.messaging.application.service;

import com.bluedigi.bluememo.messaging.application.port.out.IncomingEventRepository;
import com.bluedigi.bluememo.messaging.application.port.out.SendMessagePort;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.common.exception.StatusCodeError;
import com.bluedigi.bluememo.identity.application.service.ChannelAccountService;
import com.bluedigi.bluememo.messaging.application.exception.MessageException;
import com.bluedigi.bluememo.messaging.application.mapper.IncomingMessageMapper;
import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;
import com.bluedigi.bluememo.messaging.domain.IncomingEvent;
import com.bluedigi.bluememo.messaging.domain.enums.IncomingEventStatus;
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
        try {
            String reply = processReply(message);

            updateStatus(message, IncomingEventStatus.PROCESSED);

            OutgoingMessage toSend = new OutgoingMessage(
                    message.channelType(),
                    message.externalMessageId(),
                    message.conversationId(),
                    reply
            );

            sender.send(toSend);
            updateStatus(message, IncomingEventStatus.ANSWERED);

        } catch (RuntimeException exception) {
            try {
                updateStatus(message, IncomingEventStatus.FAILED);
            } catch (RuntimeException statusException) {
                exception.addSuppressed(statusException);
            }

            throw new MessageException(
                    StatusCodeError.INTERNAL_SERVER_ERROR.getStatusCode(),
                    exception
            );
        }
    }

    private void updateStatus(IncomingMessage message, IncomingEventStatus status) {
        IncomingEvent updateToProcessed = mapper.incomingMessageToIncomingEvent(message, status);
        eventRepository.updateIncomingEventStatus(updateToProcessed);
    }

    private String processReply(IncomingMessage message) {
        if (message.text() == null || message.text().isEmpty()) {
            return "De momento solo proceso texto";
        }

        log.info("Processing command: {}", message.action());

        return switch (message.action()) {
            case MESSAGE -> sendMessage(message.text());
            case WELCOME -> sendWelcome();
            case LINK_CHANNEL -> linkAccount(message);
            case CHECK_CHANNEL_LINK -> checkLink(message);
            case HELP -> "Estos son los comandos disponibles...";
            case UNKNOWN_COMMAND -> "Comando no reconocido";
        };
    }

    private String sendMessage(String message) {
        return "Recibí " + message;
    }

    private String sendWelcome() {
        return "Bienvenido, soy un bot";
    }

    private String linkAccount(IncomingMessage message) {

        if (!message.privateConversation()) {
            return "La vinculación solo está disponible en una conversación privada";
        }

        String externalUserId = message.senderId();
        String externalChatId = message.conversationId();
        return channelAccountService.linkAccount(externalUserId, message.channelType(), externalChatId, message.text());
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
                ? "Tu cuenta de %s está vinculada a BlueMemo.".formatted(message.channelType().getLabel())
                : "Tu cuenta de %s no está vinculada a BlueMemo.".formatted(message.channelType().getLabel());
    }
}
