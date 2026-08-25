package com.bluedigi.bluememo.messaging.application.service;

import com.bluedigi.bluememo.messaging.application.port.out.SendMessagePort;
import com.bluedigi.bluememo.messaging.domain.OutgoingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.messaging.application.port.in.ProcessIncomingMessageUseCase;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;

@Slf4j
@Service
public class ProcessIncomingMessageService implements ProcessIncomingMessageUseCase {
    private final SendMessagePort sender;

    public ProcessIncomingMessageService(SendMessagePort sender) {
        this.sender = sender;
    }


    @Override
    public void process(IncomingMessage message) {

        String reply = processReply(message.text());

        OutgoingMessage toSend = new OutgoingMessage(
                message.channelType(),
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
 