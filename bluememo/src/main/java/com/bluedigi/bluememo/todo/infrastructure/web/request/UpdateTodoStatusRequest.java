package com.bluedigi.bluememo.todo.infrastructure.web.request;

import java.util.UUID;

public record UpdateTodoStatusRequest(
    UUID userId,
    String title,
    String status
) implements TodoToolRequest {}
