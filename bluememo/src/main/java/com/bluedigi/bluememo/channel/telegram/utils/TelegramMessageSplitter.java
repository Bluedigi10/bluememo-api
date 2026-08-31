package com.bluedigi.bluememo.channel.telegram.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TelegramMessageSplitter {
    private static final int MAX_LENGTH = 4096;
    
    public List<String> split(String text) {
        
        if (text.length() <= MAX_LENGTH) {
            return List.of(text);
        }

        List<String> finalMessages = new ArrayList<>();

        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + MAX_LENGTH, text.length());

            if (end < text.length()) {
                SplitPoint points = findSplitPosition(text, start, end);

                finalMessages.add(text.substring(start, points.end()));

                start = points.nextStart();
            } else {
                finalMessages.add(text.substring(start, end));

                start = end;
            }
        }

        return finalMessages;
        
    }

    private SplitPoint findSplitPosition(String text, int start, int end) {
        int split = text.lastIndexOf("\n\n", end);

        if (split > start) {
            return new SplitPoint(split, split + 2);
        }

        split = text.lastIndexOf('\n', end);

        if (split > start) {
            return new SplitPoint(split, split + 1);
        }

        split = text.lastIndexOf(' ', end);

        if (split > start) {
            return new SplitPoint(split, split + 1);
        }

        return new SplitPoint(end, end);
    }

    private record SplitPoint(
        int end,
        int nextStart
    ) {}
    
}
