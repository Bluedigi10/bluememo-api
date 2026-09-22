package com.bluedigi.bluememo.messaging.application.service;

import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.messaging.application.formatter.ToolResultFormatter;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;
import com.bluedigi.bluememo.tool.application.port.in.ToolExecutor;
import com.bluedigi.bluememo.tool.domain.ToolExecutionResult;
import com.bluedigi.bluememo.tool.domain.ToolResult;
import com.bluedigi.bluememo.tool.domain.enums.ToolResultStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ToolMessageService {
    private final ToolExecutor toolExecutor;
    private final ToolResultFormatterResolver formatterResolver;

    public String execute(IncomingMessage message) {
        ToolExecutionResult<?> execution = toolExecutor.execute(message);

        return format(execution);
    }

    private <T> String format(ToolExecutionResult<T> execution) {
        ToolResult<T> result = execution.result();

        if (result.status() == ToolResultStatus.ERROR) {
            return result.error().message();
        }

        ToolResultFormatter<?> formatter = formatterResolver.resolve(execution.type());

        return format(formatter, execution.result());
    }

    @SuppressWarnings("unchecked")
    private <T> String format(ToolResultFormatter<?> formatter, ToolResult<T> result) {
        ToolResultFormatter<T> typedFormatter = (ToolResultFormatter<T>) formatter;

        return typedFormatter.format(result);
    }
}
