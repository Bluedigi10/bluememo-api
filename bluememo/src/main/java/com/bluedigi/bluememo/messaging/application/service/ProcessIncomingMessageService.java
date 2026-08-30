package com.bluedigi.bluememo.messaging.application.service;

import com.bluedigi.bluememo.messaging.application.port.out.IncomingEventRepository;
import com.bluedigi.bluememo.messaging.application.port.out.SendMessagePort;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public void process(IncomingMessage message) {

        String reply = processReply(message.text());

        IncomingEvent updateToProcessed = mapper.incomingMessageToIncomingEvent(message, IncomingEventStatus.PROCESSED);

        eventRepository.updateIncomingEventStatus(updateToProcessed);

        OutgoingMessage toSend = new OutgoingMessage(
                message.channelType(),
                message.externalMessageId(),
                message.conversationId(),
                reply
        );

        sender.send(toSend);
    }

    private String processReply(String message) {
        if (message == null || message.isEmpty()){
            return "De momento solo proceso texto";
        }

        if (!message.startsWith("/")) {
            return "Recibí " + message;
        }

        log.info("Processing command: {}", message);

        return switch (message) {
            case "/start" -> "Bienvenido, soy un bot";
            case "/help" -> "Estos son los comandos disponibles...";
            default -> "Comando no reconocido";
        };
    }
    
}
 