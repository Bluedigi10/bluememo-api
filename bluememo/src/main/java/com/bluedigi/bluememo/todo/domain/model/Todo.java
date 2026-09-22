package com.bluedigi.bluememo.todo.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.bluedigi.bluememo.todo.domain.enums.TodoStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Todo {
    private UUID todoId;
    private UUID userId;
    private String title;
    private String description;
    private TodoStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
