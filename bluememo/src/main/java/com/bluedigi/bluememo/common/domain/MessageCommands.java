package com.bluedigi.bluememo.common.domain;

public enum MessageCommands {
    START("/start"),
    CHECK_LINK("/check-link"),
    HELP("/help"),
    UNKNOWN("unknown");

    private final String value;

    MessageCommands(String string) {
        this.value = string;
    }

    public String getValue() {
        return this.value;
    }

    public static MessageCommands fromValue(String text) {
        for (MessageCommands command : MessageCommands.values()) {
            if (command.getValue().equals(text) && command != UNKNOWN) {
                return command;
            }
        }
        return UNKNOWN;
    }
}
