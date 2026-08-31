package com.bluedigi.bluememo.channel.telegram.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TelegramMessageSplitter {

    private static final int MAX_LENGTH = 4096;

    public List<String> split(String text) {

        if (text.codePointCount(0, text.length()) <= MAX_LENGTH) {
            return List.of(text);
        }

        List<String> messages = new ArrayList<>();

        int start = 0;

        while (start < text.length()) {

            int remainingCodePoints = text.codePointCount(start, text.length());

            int end = remainingCodePoints <= MAX_LENGTH
                    ? text.length()
                    : text.offsetByCodePoints(start, MAX_LENGTH);

            if (end < text.length()) {
                end = findSplitPosition(text, start, end);
            }

            messages.add(text.substring(start, end));

            start = end;
        }

        return messages;
    }

    private int findSplitPosition(String text, int start, int end) {
        int split = text.lastIndexOf("\n\n", end - 2);

        if (split > start) {
            return split + 2;
        }

        split = text.lastIndexOf('\n', end - 1);

        if (split > start) {
            return split + 1;
        }

        split = text.lastIndexOf(' ', end - 1);

        if (split > start) {
            return split + 1;
        }

        return end;
    }
}
