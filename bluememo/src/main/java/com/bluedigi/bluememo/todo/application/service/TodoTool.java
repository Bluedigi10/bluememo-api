package com.bluedigi.bluememo.todo.application.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.todo.application.utils.TodoRequestCreator;
import com.bluedigi.bluememo.todo.domain.enums.TodoToolAction;
import com.bluedigi.bluememo.todo.domain.repository.TodoRepository;
import com.bluedigi.bluememo.todo.infrastructure.web.request.CreateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.DeleteTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.GetTodosRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.TodoToolRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoStatusRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoPageResponse;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoToolResponse;
import com.bluedigi.bluememo.tool.application.port.out.Tool;
import com.bluedigi.bluememo.tool.domain.ToolError;
import com.bluedigi.bluememo.tool.domain.ToolExecutionContext;
import com.bluedigi.bluememo.tool.domain.ToolResult;
import com.bluedigi.bluememo.tool.domain.enums.ToolErrorCode;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TodoTool implements Tool<TodoToolRequest, TodoToolResponse> {
    private final TodoRequestCreator requestCreator;
    private final TodoService todoService;
    private final TodoRepository todoRepository;

    @Override
    public ToolType getToolType() {
        return ToolType.TODO;
    }

    @Override
    public TodoToolRequest createRequest(ToolExecutionContext request) {
        TodoToolAction action = TodoToolAction.selectAction(request.action());

        return switch (action) {
            case CREATE -> requestCreator.createCreateTodoRequest(request);
            case DELETE -> requestCreator.createDeleteTodoRequest(request);
            case GET -> requestCreator.createGetTodosRequest(request);
            case UPDATE -> requestCreator.createUpdateTodoRequest(request);
            case SET -> requestCreator.createUpdateTodoStatusRequest(request);
            case UNKNOWN -> null;
        };
    }

    @Override
    public ToolResult<TodoToolResponse> execute(TodoToolRequest request) {

        if (request == null) {
            return ToolResult.failure(generateError(ToolErrorCode.NOT_FOUND, "comando no conocido"));
        }
        if (request instanceof CreateTodoRequest create) {
            return create(create);
        }

        if (request instanceof UpdateTodoRequest update) {
            return update(update);
        }

        if (request instanceof DeleteTodoRequest delete) {
            return delete(delete);
        }

        if (request instanceof UpdateTodoStatusRequest set) {
            return set(set);
        }

        if (request instanceof GetTodosRequest get) {
            return get(get);
        }

        return ToolResult.failure(
                generateError(
                        ToolErrorCode.NOT_FOUND,
                        "comando no conocido"
                )
        );
    }

    private ToolResult<TodoToolResponse> create(CreateTodoRequest request) {

        TodoResponse todo = todoService.createTodo(request.userId(), request);

        return ToolResult.success(todo);
    }

    private ToolResult<TodoToolResponse> update(UpdateTodoRequest request) {

        UUID todoId = todoRepository.getTodoIdByUserIdAndTitle(request.userId(), request.title());

        TodoResponse todo = todoService.updateTodo(request.userId(), todoId, request);

        return ToolResult.success(todo);
    }

    private ToolResult<TodoToolResponse> set(UpdateTodoStatusRequest request) {

        UUID todoId = todoRepository.getTodoIdByUserIdAndTitle(request.userId(), request.title());

        TodoResponse todo = todoService.updateStatus(request.userId(), todoId, request.status().toString());

        return ToolResult.success(todo);
    }

    private ToolResult<TodoToolResponse> delete(DeleteTodoRequest request) {

        UUID todoId = todoRepository.getTodoIdByUserIdAndTitle(request.userId(), request.title());

        todoService.deleteTodo(request.userId(), todoId);

        return ToolResult.success(null);
    }

    private ToolResult<TodoToolResponse> get(GetTodosRequest request) {

        TodoPageResponse todo = todoService.getTodos(request.userId(), null, request.pageable());

        return ToolResult.success(todo);
    }

    private ToolError generateError(ToolErrorCode code, String message) {
        return new ToolError(
            code,
            message
        );
    }

}
