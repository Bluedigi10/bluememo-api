package com.bluedigi.bluememo.todo.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bluedigi.bluememo.todo.domain.model.Todo;

public interface TodoRepository {
    Todo saveTodo(Todo request, UUID userId);
    Todo updateTodo(Todo request);
    void deleteTodo(UUID todoId);
    void deleteTodosByUserId(UUID userId);
    boolean existByUserIdAndTitle(UUID userId, String title);
    boolean existByUserIdAndTodoId(UUID userId, UUID todoId);
    boolean existByUserIdAndTitleAndTodoIdNot(UUID userId, String title, UUID todoId);
    Optional<Todo> getById(UUID todoId);
    Page<Todo> getTodosByUserId(UUID userId, String status, Pageable pageable);
    UUID getTodoIdByUserIdAndTitle(UUID userId, String title);
}
