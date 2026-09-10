package com.bluedigi.bluememo.todo.infrastructure.persistence.mapper;

import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.identity.infrastructure.persistence.entity.UserEntity;
import com.bluedigi.bluememo.todo.domain.model.Todo;
import com.bluedigi.bluememo.todo.infrastructure.persistence.entity.TodoEntity;
import com.bluedigi.bluememo.todo.infrastructure.web.request.CreateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;

@Component
public class TodoMapper {

    private final ZoneId MEXICO_CITY = ZoneId.of("America/Mexico_City");

    public Todo createTodoRequestToTodo(CreateTodoRequest request) {
        Todo model = new Todo();

        model.setTitle(request.title().trim());
        model.setDescription(request.description());

        return model;
    }

    public Todo todoEntityToTodo(TodoEntity entity) {
        return Todo.builder()
            .todoId(entity.getTodoId())
            .userId(entity.getUser().getId())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .status(entity.getStatus())
            .createdAt(Date.from(entity.getCreatedAt().atZone(MEXICO_CITY).toInstant()))
            .updatedAt(Date.from(entity.getUpdatedAt().atZone(MEXICO_CITY).toInstant()))
            .build();
    }

    public TodoEntity todoToTodoEntity(Todo model, UserEntity user) {
        TodoEntity entity = new TodoEntity();
        entity.setTodoId(model.getTodoId());
        entity.setUser(user);
        entity.setTitle(model.getTitle());
        entity.setDescription(model.getDescription());
        entity.setStatus(model.getStatus());
        return entity;
    }

    public TodoResponse todoToTodoResponse(Todo model) {
        return new TodoResponse(
            model.getTodoId(),
            model.getTitle(),
            model.getDescription(),
            model.getStatus(),
            model.getCreatedAt(),
            model.getUpdatedAt()
        );
    }

    public void updateTodoEntity(TodoEntity current, Todo update) {
        current.setTitle(update.getTitle());
        current.setDescription(update.getDescription());
        current.setStatus(update.getStatus());
    }

}
