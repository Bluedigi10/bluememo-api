package com.bluedigi.bluememo.todo.infrastructure.web.request;

import java.util.UUID;

import com.bluedigi.bluememo.todo.domain.enums.TodoStatus;

public record UpdateTodoStatusRequest(
    UUID userId,
    String title,
    TodoStatus status
) implements TodoToolRequest {}
