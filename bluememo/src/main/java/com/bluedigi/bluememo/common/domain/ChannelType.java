package com.bluedigi.bluememo.common.domain;

import java.util.Locale;

public enum ChannelType {
    TELEGRAM,
    WHATSAPP;

    public String getLabel() {
        String value = name()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');

        return Character.toUpperCase(value.charAt(0))
                + value.substring(1);
    }
}
