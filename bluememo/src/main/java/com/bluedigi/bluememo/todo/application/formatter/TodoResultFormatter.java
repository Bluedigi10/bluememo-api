package com.bluedigi.bluememo.todo.application.formatter;

import org.springframework.stereotype.Component;

import com.bluedigi.bluememo.common.domain.ToolType;
import com.bluedigi.bluememo.messaging.application.formatter.ToolResultFormatter;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;
import com.bluedigi.bluememo.tool.domain.ToolResult;
import com.bluedigi.bluememo.tool.domain.enums.ToolResultStatus;

@Component
public class TodoResultFormatter implements ToolResultFormatter<TodoResponse>{
    @Override
    public ToolType getToolType() {
        return ToolType.TODO;
    }

    @Override
    public String format(ToolResult<TodoResponse> result) {

        if (result.status() == ToolResultStatus.ERROR) {
            return result.error().message();
        }

        TodoResponse response = result.data();

        return "Tarea creada: " + response.title();
    }
}
