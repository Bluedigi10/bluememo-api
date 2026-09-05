package com.bluedigi.bluememo.messaging.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ChannelType {
    TELEGRAM,
    WHATSAPP;

    public static ChannelType fromValue(String value) {
        if (value == null){
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid status: " + value
            );
        }
        return switch (value.toUpperCase()) {
            case "TELEGRAM" -> TELEGRAM;
            case "WHATSAPP" -> WHATSAPP;
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid status: " + value
            );
        };
    }
}
