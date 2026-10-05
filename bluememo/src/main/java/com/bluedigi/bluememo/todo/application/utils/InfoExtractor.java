package com.bluedigi.bluememo.todo.application.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.tool.domain.ToolExecutionContext;

@Component
public class InfoExtractor {

    public String extractText(ToolExecutionContext request, String key) {
        Pattern pattern = Pattern.compile(Pattern.quote(key) + ":\\s*([^;]+)");

        Matcher matcher = pattern.matcher(request.context());

        return matcher.find()
                ? matcher.group(1).trim()
                : null;
    }
}
