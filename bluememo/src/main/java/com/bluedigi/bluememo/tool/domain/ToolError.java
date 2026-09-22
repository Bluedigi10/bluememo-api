package com.bluedigi.bluememo.tool.domain;

import com.bluedigi.bluememo.tool.domain.enums.ToolErrorCode;

public record ToolError(
    ToolErrorCode code,
    String message
) {
}
