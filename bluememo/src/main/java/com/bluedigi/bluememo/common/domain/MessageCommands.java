package com.bluedigi.bluememo.common.domain;

public enum MessageCommands {
    START("/start"),
    HELP("/help");

    private String value;

    MessageCommands(String string) {
        this.value = string;
    }

    public String getValue() {
        return this.value;
    }

    public static MessageCommands fromValue(String text) {
        for (MessageCommands command : MessageCommands.values()) {
            if (command.getValue().equals(text)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Invalid command value: " + text);
    }


}
