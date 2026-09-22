package com.bluedigi.bluememo.todo.infrastructure.web.request;

public sealed interface TodoToolRequest permits CreateTodoRequest,
                                                DeleteTodoRequest,
                                                GetTodosRequest,
                                                UpdateTodoRequest,
                                                UpdateTodoStatusRequest {

}
