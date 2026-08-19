package com.bluedigi.bluememo.todo.domain.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum TodoStatus {
    PENDING,
    IN_PROGRESS,
    DONE;

    public static TodoStatus fromValue(String value) {
        if (value == null){
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid status: " + value
            );
        }
        return switch (value.toUpperCase()) {
            case "IN PROGRESS", "IN_PROGRESS" -> IN_PROGRESS;
            case "PENDING" -> PENDING;
            case "DONE", "COMPLETED" -> DONE;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid status: " + value
            );
        };
    }
}
