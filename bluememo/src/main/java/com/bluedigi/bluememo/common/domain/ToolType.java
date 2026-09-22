package com.bluedigi.bluememo.common.domain;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum ToolType {
    TODO("/todo"),
    CALENDAR("/calendar"),
    SPOTIFY("/spotify"),
    UNKNOWN("");



    private final String command;

    ToolType(String command) {
        this.command = command;
    }

    public static ToolType selectTool(String message) {

        return Arrays.stream(values())
                .filter(tool -> tool.command.equals(message.toLowerCase()))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
