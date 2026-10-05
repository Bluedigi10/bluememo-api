package com.bluedigi.bluememo.todo.infrastructure.web;

import org.springframework.web.bind.annotation.RestController;

import com.bluedigi.bluememo.todo.application.service.TodoService;
import com.bluedigi.bluememo.todo.domain.enums.TodoSortField;
import com.bluedigi.bluememo.todo.infrastructure.web.request.CreateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoPageResponse;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoService service;

    public TodoController(TodoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(
        @AuthenticationPrincipal UserDetails loggedUser,
        @Valid @RequestBody CreateTodoRequest createTodoRequest
    ) {
        UUID userId = UUID.fromString(loggedUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTodo(userId, createTodoRequest));
    }

    @GetMapping
    public ResponseEntity<TodoPageResponse> getAllTodos(
        @AuthenticationPrincipal UserDetails loggedUser,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        UUID userId = UUID.fromString(loggedUser.getUsername());

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        TodoSortField sortField = TodoSortField.fromValue(sortBy);

        Pageable pageable = PageRequest.of(
            page,
            size,
            sortDirection,
            sortField.getProperty()
        );
        TodoPageResponse response = service.getTodos(userId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{todoIdString}")
    public ResponseEntity<TodoResponse> getTodo(
        @AuthenticationPrincipal UserDetails loggedUser,
        @PathVariable String todoIdString
    ) {
        UUID userId = UUID.fromString(loggedUser.getUsername());
        UUID todoId = UUID.fromString(todoIdString);
        return ResponseEntity.ok(service.getTodo(userId, todoId));
    }


    @PutMapping("/{todoIdString}")
    public ResponseEntity<TodoResponse> updateTodo(
        @AuthenticationPrincipal UserDetails loggedUser,
        @PathVariable String todoIdString,
        @Valid @RequestBody UpdateTodoRequest request
    ) {
        UUID userId = UUID.fromString(loggedUser.getUsername());
        UUID todoId = UUID.fromString(todoIdString);
        return ResponseEntity.ok(service.updateTodo(userId, todoId, request));
    }

    @PatchMapping("/{todoIdString}")
    public ResponseEntity<TodoResponse> updateStatus(
        @AuthenticationPrincipal UserDetails loggedUser,
        @PathVariable String todoIdString,
        @RequestParam String status
    ) {
        UUID userId = UUID.fromString(loggedUser.getUsername());
        UUID todoId = UUID.fromString(todoIdString);
        return ResponseEntity.ok(service.updateStatus(userId, todoId, status));
    }

    @DeleteMapping("/{todoIdString}")
    public ResponseEntity<Void> deleteTodo (
        @AuthenticationPrincipal UserDetails loggedUser,
        @PathVariable String todoIdString
    ) {
        UUID userId = UUID.fromString(loggedUser.getUsername());
        UUID todoId = UUID.fromString(todoIdString);
        service.deleteTodo(userId, todoId);

        return ResponseEntity.noContent().build();
    }

}
