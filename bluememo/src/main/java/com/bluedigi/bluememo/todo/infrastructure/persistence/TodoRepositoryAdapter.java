package com.bluedigi.bluememo.todo.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.bluedigi.bluememo.todo.domain.model.Todo;
import com.bluedigi.bluememo.todo.domain.model.TodoStatus;
import com.bluedigi.bluememo.todo.domain.repository.TodoRepository;
import com.bluedigi.bluememo.todo.infrastructure.persistence.entity.TodoEntity;
import com.bluedigi.bluememo.todo.infrastructure.persistence.mapper.TodoMapper;
import com.bluedigi.bluememo.todo.infrastructure.persistence.repository.TodoJpaRepository;

@Repository
public class TodoRepositoryAdapter implements TodoRepository{

    private final TodoJpaRepository todoJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TodoMapper todoMapper;

    public TodoRepositoryAdapter (TodoJpaRepository todoJpaRepository, UserJpaRepository userJpaRepository, TodoMapper todoMapper) {
        this.todoJpaRepository = todoJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.todoMapper = todoMapper;
    }

    @Override
    public Todo saveTodo(Todo request, UUID userId) {
        UserEntity user = userJpaRepository.getReferenceById(userId);
        TodoEntity todoToSave = todoMapper.todoToTodoEntity(request, user);

        TodoEntity saved;

        try {
            saved = todoJpaRepository.saveAndFlush(todoToSave);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Todo title already exist");
        }

        return todoMapper.todoEntityToTodo(saved);
    }

    @Override
    public Todo updateTodo(Todo request) {
        TodoEntity existing = todoJpaRepository.findById(request.getTodoId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found"));
        todoMapper.updateTodoEntity(existing, request);

        TodoEntity updated;

        try {
            updated = todoJpaRepository.saveAndFlush(existing);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unexpected error occurred while updating the todo");
        }

        return todoMapper.todoEntityToTodo(updated);
    }

    @Override
    public void deleteTodo(UUID todoId) {
        todoJpaRepository.deleteById(todoId);
    }

        @Override
    public void deleteTodosByUserId(UUID userId) {
        todoJpaRepository.deleteAllByUserId(userId);
    }

    @Override
    public boolean existByUserIdAndTitle(UUID userId, String title) {
        return todoJpaRepository.existsByUser_IdAndTitle(userId, title);
    }

    @Override
    public boolean existByUserIdAndTodoId(UUID userId, UUID todoId) {
        return todoJpaRepository.existsByUser_IdAndTodoId(userId, todoId);
    }

    @Override
    public boolean existByUserIdAndTitleAndTodoIdNot(UUID userId, String title, UUID todoId) {
        return todoJpaRepository.existsByUser_IdAndTitleAndTodoIdNot(userId, title, todoId);
    }

    @Override
    public Optional<Todo> getById(UUID todoId) {
        return todoJpaRepository.findById(todoId).map(todoMapper::todoEntityToTodo);
    }

    @Override
    public Page<Todo> getTodosByUserId(UUID userId, String status, Pageable pageable) {

        if (status == null || status.isBlank()) {
            return todoJpaRepository.findAllByUser_Id(userId, pageable)
                    .map(todoMapper::todoEntityToTodo);
        }

        TodoStatus todoStatus = TodoStatus.fromValue(status);

        return todoJpaRepository
                .findAllByUser_IdAndStatus(userId, todoStatus, pageable)
                .map(todoMapper::todoEntityToTodo);
    }
}
