package com.bluedigi.bluememo.messaging.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.messaging.application.formatter.ToolResultFormatter;

@Component
public class ToolResultFormatterResolver {
    private final Map<ToolType, ToolResultFormatter<?>> formatters;

    public ToolResultFormatterResolver(List<ToolResultFormatter<?>> formatters) {
        this.formatters = formatters.stream()
                .collect(Collectors.toMap(
                        ToolResultFormatter::getToolType,
                        Function.identity()
                ));
    }

    @SuppressWarnings("java:S1452")
    public ToolResultFormatter<?> resolve(ToolType type) {
        return formatters.get(type);
    }
}
