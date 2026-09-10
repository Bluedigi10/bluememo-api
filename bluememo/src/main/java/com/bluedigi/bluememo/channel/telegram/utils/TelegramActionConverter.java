package com.bluedigi.bluememo.channel.telegram.utils;

import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramAction;
import com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request.TelegramCommands;
import com.bluedigi.bluememo.messaging.domain.enums.IncomingAction;
import org.springframework.stereotype.Component;

@Component
public class TelegramActionConverter {
    public TelegramAction parseAction(String message){
        String text = message;
        if (text == null || !text.startsWith("/")) {
            return new TelegramAction(IncomingAction.MESSAGE, text);
        }

        String[] parts = extractCommandFromMessage(text);
        String command = parts[0];
        String instruction = parts.length > 1 ? parts[1] : null; //action that will be executed

        IncomingAction action = switch (TelegramCommands.fromValue(command)) {
            case START -> validateStart(instruction);
            case CHECK_LINK -> IncomingAction.CHECK_CHANNEL_LINK;
            case HELP -> IncomingAction.HELP;
            default -> IncomingAction.UNKNOWN_COMMAND;
        };

        if (instruction != null && !instruction.isBlank()) {
            text = instruction;
        }

        return new TelegramAction(action, text);
    }


    private IncomingAction validateStart(String token) {
        if (token == null || token.isBlank()) {
            return IncomingAction.WELCOME;
        }

        return IncomingAction.LINK_CHANNEL;
    }

    private String[] extractCommandFromMessage(String text) {
        return text.trim().split("\\s+", 2);
    }
}
