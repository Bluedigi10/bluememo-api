package com.bluedigi.bluememo.todo.infrastructure.web.request;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

public record GetTodosRequest (
    UUID userId,
    Pageable pageable
) implements TodoToolRequest {}
