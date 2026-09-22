package com.bluedigi.bluememo.todo.domain.enums;

import java.util.Arrays;

public enum TodoToolAction {
    CREATE("create"),
    GET("get"),
    UPDATE("update"),
    SET("set"),
    DELETE("delete"),
    UNKNOWN("");

    private final String action;

    TodoToolAction(String action) {
        this.action = action;
    }

    public static TodoToolAction selectAction(String message) {

        return Arrays.stream(values())
                .filter(action -> action.action.equals(message.toLowerCase()))
                .findFirst()
                .orElse(UNKNOWN);
    }

}
