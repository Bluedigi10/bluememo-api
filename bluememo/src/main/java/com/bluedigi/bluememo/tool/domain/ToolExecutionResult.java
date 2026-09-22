package com.bluedigi.bluememo.tool.domain;

import com.bluedigi.bluememo.common.domain.ToolType;

public record ToolExecutionResult<T>(
    ToolType type,
    ToolResult<T> result
) {
}
