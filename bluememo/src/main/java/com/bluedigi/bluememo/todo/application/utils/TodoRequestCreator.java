package com.bluedigi.bluememo.todo.application.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.todo.domain.model.Todo;
import com.bluedigi.bluememo.todo.infrastructure.web.request.CreateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.DeleteTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.GetTodosRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoStatusRequest;
import com.bluedigi.bluememo.tool.domain.ToolExecutionContext;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TodoRequestCreator {
    private final InfoExtractor infoExtractor;
    public CreateTodoRequest createCreateTodoRequest(ToolExecutionContext request) {
        Todo todo = createTodoFromRequest(request);
        return new CreateTodoRequest(
            request.userId(),
            todo.getTitle(),
            todo.getDescription()
        );
    }

    public DeleteTodoRequest createDeleteTodoRequest(ToolExecutionContext request) {
        Todo todo = createTodoFromRequest(request);
        return new DeleteTodoRequest(
            request.userId(),
            todo.getTitle()
        );
    }

    public GetTodosRequest createGetTodosRequest(ToolExecutionContext request) {
        Pageable pageable = PageRequest.of(
            0,
            10,
            Sort.Direction.DESC,
            "title"
        );
        return new GetTodosRequest(
            request.userId(),
            pageable
        );
    }

    public UpdateTodoRequest createUpdateTodoRequest(ToolExecutionContext request) {
        Todo todo = createTodoFromRequest(request);
        return new UpdateTodoRequest(
            request.userId(),
            todo.getTitle(),
            todo.getDescription()
        );
    }

    public UpdateTodoStatusRequest createUpdateTodoStatusRequest(ToolExecutionContext request) {
        Todo todo = createTodoFromRequest(request);
        return new UpdateTodoStatusRequest(
            request.userId(),
            todo.getTitle(),
            todo.getStatus()
        );
    }

    private Todo createTodoFromRequest(ToolExecutionContext request) {
        String title = infoExtractor.extractText(request, "title");
        String description = infoExtractor.extractText(request, "description");
        return Todo.builder()
            .userId(request.userId())
            .title(title)
            .description(description)
            .build();
    }
}
