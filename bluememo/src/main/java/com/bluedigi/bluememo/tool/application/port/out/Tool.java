package com.bluedigi.bluememo.tool.application.port.out;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.tool.domain.ToolExecutionContext;
import com.bluedigi.bluememo.tool.domain.ToolResult;

public interface Tool<R, S> {
    ToolType getToolType();
    R createRequest(ToolExecutionContext request);
    ToolResult<S> execute(R request);
}
