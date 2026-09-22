package com.bluedigi.bluememo.tool.domain;

import java.util.UUID;

import com.bluedigi.bluememo.common.domain.ToolType;

public record ToolExecutionContext(
    UUID userId,
    ToolType type,
    String action,
    String context
) {}
