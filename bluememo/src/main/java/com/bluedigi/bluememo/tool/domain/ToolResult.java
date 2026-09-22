package com.bluedigi.bluememo.tool.domain;

import com.bluedigi.bluememo.tool.domain.enums.ToolResultStatus;

public record ToolResult<T>(
    ToolResultStatus status,
    T data,
    ToolError error
) {
    public static <T> ToolResult<T> success(T data) {
        return new ToolResult<>(ToolResultStatus.SUCCESS, data, null);
    }

    public static <T> ToolResult<T> failure(ToolError error) {
        return new ToolResult<>(ToolResultStatus.ERROR, null, error);
    }
}
