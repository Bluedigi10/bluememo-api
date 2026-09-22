package com.bluedigi.bluememo.todo.infrastructure.web.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTodoRequest(
    UUID userId,
    @NotBlank
    @Size(max = 255)
    String title,
    @NotNull
    @Size(max = 255)
    String description
) implements TodoToolRequest {
}
