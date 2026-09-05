package com.bluedigi.bluememo.todo.application.service;

import java.util.UUID;

import com.bluedigi.bluememo.todo.domain.model.TodoSortField;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bluedigi.bluememo.identity.domain.repository.UserRepository;
import com.bluedigi.bluememo.todo.domain.model.Todo;
import com.bluedigi.bluememo.todo.domain.model.TodoStatus;
import com.bluedigi.bluememo.todo.domain.repository.TodoRepository;
import com.bluedigi.bluememo.todo.infrastructure.persistence.mapper.TodoMapper;
import com.bluedigi.bluememo.todo.infrastructure.web.request.CreateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    public TodoService(TodoMapper todoMapper, TodoRepository todoRepository, UserRepository userRepository) {
        this.todoMapper = todoMapper;
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TodoResponse createTodo(String userId, CreateTodoRequest request) {
        UUID userUuid = UUID.fromString(userId);

        validateUserId(userUuid);

        if (todoRepository.existByUserIdAndTitle(userUuid, request.title().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Todo already exist");
        }

        Todo todoSave = todoMapper.createTodoRequestToTodo(request);

        todoSave.setStatus(TodoStatus.PENDING);

        Todo todoSaved = todoRepository.saveTodo(todoSave, userUuid);

        return todoMapper.todoToTodoResponse(todoSaved);
    }

    @Transactional(readOnly = true)
    public Page<TodoResponse> getTodos(String userId, String status, String sortBy, String direction, int page, int size) {
        UUID userUuid = UUID.fromString(userId);

        validateUserId(userUuid);
        Sort.Direction sortDirection =
            Sort.Direction.fromString(direction);
        TodoSortField sortField = TodoSortField.fromValue(sortBy);

        Pageable pageable = PageRequest.of(
            page,
            size,
            sortDirection,
            sortField.getProperty()
        );

        return todoRepository.getTodosByUserId(userUuid, status, pageable).map(todoMapper::todoToTodoResponse);
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodo(String userId, String todoId) {
        UUID userUuid = UUID.fromString(userId);
        UUID todoUuid = UUID.fromString(todoId);

        validateUserId(userUuid);
        validateTodoAndUser(userUuid, todoUuid);

        Todo todo = todoRepository.getById(todoUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));

        return todoMapper.todoToTodoResponse(todo);
    }

    @Transactional
    public TodoResponse updateTodo(String userId, String todoId, UpdateTodoRequest request) {
        UUID userUuid = UUID.fromString(userId);
        UUID todoUuid = UUID.fromString(todoId);
        String title = request.title().trim();

        validateUserId(userUuid);
        validateTodoAndUser(userUuid, todoUuid);
        validateTodoIdAndTitleAndUserId(userUuid, title, todoUuid);
        if (isInvalidString(title) && isInvalidString(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Parameter");
        }

        Todo existingTodo = todoRepository.getById(todoUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));

        existingTodo.setTitle(title);
        existingTodo.setDescription(request.description());


        return todoMapper.todoToTodoResponse(todoRepository.updateTodo(existingTodo));
    }

    @Transactional
    public TodoResponse updateStatus(String userId, String todoId, String status){

        UUID userUuid = UUID.fromString(userId);
        UUID todoUuid = UUID.fromString(todoId);

        validateUserId(userUuid);
        validateTodoAndUser(userUuid, todoUuid);

        Todo existingTodo = todoRepository.getById(todoUuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));

        TodoStatus newStatus = TodoStatus.fromValue(status);

        if (existingTodo.getStatus() == newStatus) {
            return todoMapper.todoToTodoResponse(existingTodo);
        }

        existingTodo.setStatus(newStatus);



        return todoMapper.todoToTodoResponse(todoRepository.updateTodo(existingTodo));
    }

    @Transactional
    public void deleteTodo(String userId, String todoId) {
        UUID userUuid = UUID.fromString(userId);
        UUID todoUuid = UUID.fromString(todoId);

        validateUserId(userUuid);
        validateTodoAndUser(userUuid, todoUuid);

        todoRepository.deleteTodo(todoUuid);
    }

    private void validateUserId(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private void validateTodoAndUser(UUID userId, UUID todoId) {
        if (!todoRepository.existByUserIdAndTodoId(userId, todoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found");
        }
    }

    private void validateTodoIdAndTitleAndUserId(UUID userId, String title, UUID todoId){
        if (todoRepository.existByUserIdAndTitleAndTodoIdNot(userId, title, todoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Todo title already exist");
        }
    }

    private boolean isInvalidString(String text) {
        return text == null || text.isBlank();
    }
}
