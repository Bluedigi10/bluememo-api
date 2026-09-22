package com.bluedigi.bluememo.messaging.application.formatter;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.tool.domain.ToolResult;

public interface ToolResultFormatter<T> {
    ToolType getToolType();
    String format(ToolResult<T> result);
}
