package com.bluedigi.bluememo.todo.infrastructure.web.response;

import com.bluedigi.bluememo.common.domain.PageResponse;

public record TodoPageResponse(
    PageResponse<TodoResponse> page
) implements TodoToolResponse {
}
