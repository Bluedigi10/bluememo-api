package com.bluedigi.bluememo.channel.telegram.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TelegramMessageSplitter {

    private static final int MAX_LENGTH = 4096;
    private static final int MAX_SPLIT_DISTANCE = 50;

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
        int lineBreak = text.lastIndexOf('\n', end - 1);
        int space = text.lastIndexOf(' ', end - 1);

        int split = Math.max(lineBreak, space);

        if (split <= start) {
            return end;
        }

        int splitEnd = split + 1;

        int distanceToLimit =
                text.codePointCount(splitEnd, end);

        return distanceToLimit <= MAX_SPLIT_DISTANCE
                ? splitEnd
                : end;
        }
}
