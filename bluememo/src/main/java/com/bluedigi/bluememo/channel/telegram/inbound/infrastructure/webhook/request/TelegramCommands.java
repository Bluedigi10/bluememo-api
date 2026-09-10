package com.bluedigi.bluememo.channel.telegram.inbound.infrastructure.webhook.request;

public enum TelegramCommands {
    START("/start"),
    CHECK_LINK("/check-link"),
    HELP("/help"),
    UNKNOWN("unknown");

    private final String value;

    TelegramCommands(String string) {
        this.value = string;
    }

    public String getValue() {
        return this.value;
    }

    public static TelegramCommands fromValue(String text) {
        for (TelegramCommands command : TelegramCommands.values()) {
            if (command.getValue().equals(text) && command != UNKNOWN) {
                return command;
            }
        }
        return UNKNOWN;
    }
}
