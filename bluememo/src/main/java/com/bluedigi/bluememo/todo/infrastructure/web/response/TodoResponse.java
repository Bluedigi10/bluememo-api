package com.bluedigi.bluememo.todo.infrastructure.web.response;

import java.time.Instant;
import java.util.UUID;

import com.bluedigi.bluememo.todo.domain.enums.TodoStatus;

public record TodoResponse(
    UUID todoId,
    String title,
    String description,
    TodoStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
