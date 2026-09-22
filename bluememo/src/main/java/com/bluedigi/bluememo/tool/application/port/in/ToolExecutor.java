package com.bluedigi.bluememo.tool.application.port.in;

import com.bluedigi.bluememo.messaging.domain.IncomingMessage;
import com.bluedigi.bluememo.tool.domain.ToolExecutionResult;

public interface ToolExecutor {
    ToolExecutionResult<?> execute(IncomingMessage message);
}
