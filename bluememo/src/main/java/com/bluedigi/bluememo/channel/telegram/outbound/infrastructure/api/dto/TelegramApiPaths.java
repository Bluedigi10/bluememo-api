package com.bluedigi.bluememo.channel.telegram.outbound.infrastructure.api.dto;

public enum TelegramApiPaths {
    SEND_MESSAGE("/sendMessage");

    private final String path;

    TelegramApiPaths(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
