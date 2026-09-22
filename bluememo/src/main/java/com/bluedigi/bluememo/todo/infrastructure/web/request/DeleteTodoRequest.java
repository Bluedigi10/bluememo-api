package com.bluedigi.bluememo.todo.infrastructure.web.request;

import java.util.UUID;

public record DeleteTodoRequest(
    UUID userId,
    String title
) implements TodoToolRequest {}
