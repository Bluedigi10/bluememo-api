package com.bluedigi.bluememo.tool.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.common.exception.BluememoException;
import com.bluedigi.bluememo.common.exception.StatusCodeError;
import com.bluedigi.bluememo.identity.application.port.ChannelIdentityResolver;
import com.bluedigi.bluememo.messaging.domain.IncomingMessage;
import com.bluedigi.bluememo.tool.application.port.in.ToolExecutor;
import com.bluedigi.bluememo.tool.application.port.out.Tool;
import com.bluedigi.bluememo.tool.domain.ToolError;
import com.bluedigi.bluememo.tool.domain.ToolExecutionContext;
import com.bluedigi.bluememo.tool.domain.ToolExecutionResult;
import com.bluedigi.bluememo.tool.domain.ToolResult;
import com.bluedigi.bluememo.tool.domain.enums.ToolErrorCode;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class ToolExecutionService implements ToolExecutor{
    private final ToolResolver resolve;
    private final ChannelIdentityResolver identityResolver;

    public ToolExecutionResult<?> execute(IncomingMessage message){

        if (ToolType.selectTool(message.action().name()).equals(ToolType.UNKNOWN)) {
            return getUnknownTool();
        }
        UUID identity = identityResolver.resolve(message.channelType(), message.senderId(), message.conversationId()).orElseThrow(
            () -> new BluememoException("No existe la cuenta", StatusCodeError.NOT_FOUND.getStatusCode())
        );
        ToolExecutionContext contextExecution = buildContext(message, identity);

        Tool<?, ?> tool = resolve.resolve(contextExecution.type());

        return executeTool(tool, contextExecution);
    }

    private <R, S> ToolExecutionResult<S> executeTool(Tool<R, S> tool, ToolExecutionContext context) {
        R request = tool.createRequest(context);

        ToolResult<S> result = tool.execute(request);

        return new ToolExecutionResult<>(
            tool.getToolType(),
            result
        );
    }

    private ToolExecutionContext buildContext(IncomingMessage message, UUID userId) {
        String[] contextCommand = contextCommand(message.text());
        ToolType toolType = ToolType.selectTool(contextCommand[0]);
        String command = contextCommand[1];
        String context = contextCommand[2];
        return new ToolExecutionContext(
            userId,
            toolType,
            command,
            context
        );
    }

    private String[] contextCommand (String text) {
        return text.split(" ", 3);
    }

    private ToolExecutionResult<String> getUnknownTool () {
        ToolError error = new ToolError(ToolErrorCode.NOT_FOUND, "Comando no reconocido");
        ToolResult<String> result = ToolResult.failure(error);

        return new ToolExecutionResult<>(ToolType.UNKNOWN, result);
    }
}
