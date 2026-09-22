package com.bluedigi.bluememo.todo.application.service;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.todo.domain.enums.TodoToolAction;
import com.bluedigi.bluememo.todo.infrastructure.web.request.TodoToolRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;
import com.bluedigi.bluememo.tool.application.port.out.Tool;
import com.bluedigi.bluememo.tool.domain.ToolError;
import com.bluedigi.bluememo.tool.domain.ToolExecutionContext;
import com.bluedigi.bluememo.tool.domain.ToolResult;
import com.bluedigi.bluememo.tool.domain.enums.ToolErrorCode;

public class TodoTool implements Tool<TodoToolRequest, TodoResponse> {

    @Override
    public ToolType getToolType() {
        return ToolType.TODO;
    }

    @Override
    public TodoToolRequest createRequest(ToolExecutionContext request) {
        TodoToolAction action = TodoToolAction.selectAction(request.action());
        if (action.equals(TodoToolAction.UNKNOWN)) {
            return null;
        }
        throw new UnsupportedOperationException("Unimplemented method 'createRequest'");
    }

    @Override
    public ToolResult<TodoResponse> execute(TodoToolRequest request) {

        if (request == null) {
            return ToolResult.failure(generateError(ToolErrorCode.NOT_FOUND, "comando no conocido"));
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }

    private ToolError generateError(ToolErrorCode code, String message) {
        return new ToolError(
            code,
            message
        );
    }

}
