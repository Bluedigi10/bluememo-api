package com.bluedigi.bluememo.todo.application.service;

import com.bluedigi.bluememo.identity.domain.repository.UserRepository;
import com.bluedigi.bluememo.todo.domain.model.Todo;
import com.bluedigi.bluememo.todo.domain.model.TodoStatus;
import com.bluedigi.bluememo.todo.domain.repository.TodoRepository;
import com.bluedigi.bluememo.todo.infrastructure.persistence.mapper.TodoMapper;
import com.bluedigi.bluememo.todo.infrastructure.web.request.CreateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.request.UpdateTodoRequest;
import com.bluedigi.bluememo.todo.infrastructure.web.response.TodoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    private static final String TITLE = "TEST 1";
    private static final String DESCRIPTION = "DESCRIPTION 1";
    private static final Instant NOW = Instant.parse("2026-08-04T12:00:00Z");

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private UserRepository userRepository;

    private final TodoMapper todoMapper = new TodoMapper();

    private TodoService todoService;

    @BeforeEach
    void setup() {
        todoService = new TodoService(
                todoMapper,
                todoRepository,
                userRepository
        );
    }

    @Test
    void createTodoSuccess() {
        UUID userId = UUID.randomUUID();
        CreateTodoRequest request = createTodoRequest();
        Todo todoSaved = saveTodo(userId);
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTitle(userId, TITLE)).thenReturn(false);
        when(todoRepository.saveTodo(any(Todo.class), eq(userId))).thenReturn(todoSaved);

        TodoResponse response = todoService.createTodo(userIdString, request);
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).saveTodo(todoCaptor.capture(), eq(userId));
        Todo todoSentToRepository = todoCaptor.getValue();

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(todoSaved.getTodoId(), response.todoId()),
                () -> assertEquals(TITLE, response.title()),
                () -> assertEquals(DESCRIPTION, response.description()),
                () -> assertEquals(TodoStatus.PENDING, response.status()),
                () -> assertEquals(TITLE, todoSentToRepository.getTitle()),
                () -> assertEquals(DESCRIPTION, todoSentToRepository.getDescription()),
                () -> assertEquals(TodoStatus.PENDING, todoSentToRepository.getStatus())
        );

        verify(userRepository).existsById(userId);
        verify(todoRepository).existByUserIdAndTitle(userId, request.title().trim());
    }

    @Test
    void createTodoErrorTodoAlreadyExists() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        CreateTodoRequest request = createTodoRequest();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTitle(userId, TITLE)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.createTodo(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.CONFLICT, "Todo already exist");

        verify(todoRepository, never()).saveTodo(any(Todo.class), eq(userId));
    }

    @Test
    void createTodoErrorTodoUserNotFound() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        CreateTodoRequest request = createTodoRequest();

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.createTodo(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");

        verifyNoInteractions(todoRepository);
    }

    @Test
    void getTodosSuccess() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        Todo todo = createTodo(UUID.randomUUID(), userId, TITLE, DESCRIPTION);
        Page<Todo> repositoryPage = new PageImpl<>(List.of(todo));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.getTodosByUserId(eq(userId), eq("PENDING"), any(Pageable.class)))
                .thenReturn(repositoryPage);

        Page<TodoResponse> result = todoService.getTodos(
                userIdString,
                "PENDING",
                "updatedAt",
                "asc",
                2,
                5
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(todoRepository).getTodosByUserId(eq(userId), eq("PENDING"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        Sort.Order updatedAtOrder = pageable.getSort().getOrderFor("updatedAt");

        assertAll(
                () -> assertEquals(1, result.getNumberOfElements()),
                () -> assertEquals(todo.getTodoId(), result.getContent().get(0).todoId()),
                () -> assertEquals(TITLE, result.getContent().get(0).title()),
                () -> assertEquals(2, pageable.getPageNumber()),
                () -> assertEquals(5, pageable.getPageSize()),
                () -> assertNotNull(updatedAtOrder),
                () -> assertEquals(Sort.Direction.ASC, updatedAtOrder.getDirection())
        );
    }

    @Test
    void getTodosErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.getTodos(userIdString, null, "createdAt", "desc", 0, 10)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verifyNoInteractions(todoRepository);
    }

    @Test
    void getTodosErrorInvalidSortField() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.getTodos(userIdString, null, "invalid", "desc", 0, 10)
        );

        assertStatusException(exception, HttpStatus.BAD_REQUEST, "Invalid sort field: invalid");
        verifyNoInteractions(todoRepository);
    }

    @Test
    void getTodosErrorDirectionInvalid() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> todoService.getTodos(userIdString, null, "createdAt", "sideways", 0, 10)
        );

        verifyNoInteractions(todoRepository);
    }

    @Test
    void getTodoByIdSuccess() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String userIdString = userId.toString();
        Todo todo = createTodo(todoId, userId, TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.of(todo));

        TodoResponse response = todoService.getTodo(userIdString, todoId.toString());

        assertTodoResponse(response, todo);
        verify(todoRepository).getById(todoId);
    }

    @Test
    void getTodoByIdErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.getTodo(userIdString, todoString)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verifyNoInteractions(todoRepository);
    }

    @Test
    void getTodoErrorTodoNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTodoId(userId, todoId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.getTodo(userIdString, todoString)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
        verify(todoRepository, never()).getById(any(UUID.class));
    }

    @Test
    void getTodoByIdErrorGetEmptyTodo() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.getTodo(userIdString, todoString)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
    }

    @Test
    void updateTodoSuccess() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        Todo existingTodo = createTodo(todoId, userId, "OLD TITLE", "OLD DESCRIPTION");
        UpdateTodoRequest request = new UpdateTodoRequest("  " + TITLE + "  ", DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.existByUserIdAndTitleAndTodoIdNot(userId, TITLE, todoId)).thenReturn(false);
        when(todoRepository.getById(todoId)).thenReturn(Optional.of(existingTodo));
        when(todoRepository.updateTodo(any(Todo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.updateTodo(userIdString, todoString, request);

        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).updateTodo(todoCaptor.capture());
        Todo updatedTodo = todoCaptor.getValue();

        assertAll(
                () -> assertEquals(TITLE, updatedTodo.getTitle()),
                () -> assertEquals(DESCRIPTION, updatedTodo.getDescription()),
                () -> assertEquals(TITLE, response.title()),
                () -> assertEquals(DESCRIPTION, response.description())
        );
    }

    @Test
    void updateTodoErrorTitleConflict() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        UpdateTodoRequest request = new UpdateTodoRequest(TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.existByUserIdAndTitleAndTodoIdNot(userId, TITLE, todoId)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateTodo(userIdString, todoString, request)
        );

        assertStatusException(exception, HttpStatus.CONFLICT, "Todo title already exist");
        verify(todoRepository, never()).getById(any(UUID.class));
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void updateTodoErrorBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        UpdateTodoRequest request = new UpdateTodoRequest("   ", "   ");

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.existByUserIdAndTitleAndTodoIdNot(userId, "", todoId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateTodo(userIdString, todoString, request)
        );

        assertStatusException(exception, HttpStatus.BAD_REQUEST, "Invalid Parameter");
        verify(todoRepository, never()).getById(any(UUID.class));
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
        void updateTodoErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        UpdateTodoRequest request = new UpdateTodoRequest(TITLE, DESCRIPTION);

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateTodo(userIdString, todoString, request)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verifyNoInteractions(todoRepository);
    }

    @Test
    void updateTodoErrorTodoNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        UpdateTodoRequest request = new UpdateTodoRequest(TITLE, DESCRIPTION);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTodoId(userId, todoId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateTodo(userIdString, todoString, request)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void updateTodoErrorTodoNotFoundAfterValidation() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        UpdateTodoRequest request = new UpdateTodoRequest(TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.existByUserIdAndTitleAndTodoIdNot(userId, TITLE, todoId)).thenReturn(false);
        when(todoRepository.getById(todoId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateTodo(userIdString, todoString, request)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void updateStatusSuccess() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        Todo existingTodo = createTodo(todoId, userId, TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.of(existingTodo));
        when(todoRepository.updateTodo(any(Todo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoService.updateStatus(
                userIdString,
                todoString,
                "in progress"
        );

        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).updateTodo(todoCaptor.capture());

        assertAll(
                () -> assertEquals(TodoStatus.IN_PROGRESS, todoCaptor.getValue().getStatus()),
                () -> assertEquals(TodoStatus.IN_PROGRESS, response.status())
        );
    }

    @Test
    void updateStatusWithSameStatus() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        Todo existingTodo = createTodo(todoId, userId, TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.of(existingTodo));

        TodoResponse response = todoService.updateStatus(
                userIdString,
                todoString,
                "PENDING"
        );

        assertEquals(TodoStatus.PENDING, response.status());
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void updateStatusErrorStatusInvalid() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        Todo existingTodo = createTodo(todoId, userId, TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.of(existingTodo));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateStatus(userIdString, todoString, "INVALID")
        );

        assertStatusException(exception, HttpStatus.BAD_REQUEST, "Invalid status: INVALID");
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void updateStatusErrorStatusNull() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();
        Todo existingTodo = createTodo(todoId, userId, TITLE, DESCRIPTION);

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.of(existingTodo));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateStatus(userIdString, todoString, null)
        );

        assertStatusException(exception, HttpStatus.BAD_REQUEST, "Invalid status: null");
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void updateStatusErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateStatus(userIdString, todoString, "DONE")
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verifyNoInteractions(todoRepository);
    }

    @Test
    void updateStatusErrorTodoNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTodoId(userId, todoId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateStatus(userIdString, todoString, "DONE")
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
        verify(todoRepository, never()).getById(any(UUID.class));
    }

    @Test
    void updateStatusErrorTodoNotFoundAfterValidation() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        mockExistingUserAndOwnedTodo(userId, todoId);
        when(todoRepository.getById(todoId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.updateStatus(userIdString, todoString, "DONE")
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
        verify(todoRepository, never()).updateTodo(any(Todo.class));
    }

    @Test
    void deleteTodoSuccess() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        mockExistingUserAndOwnedTodo(userId, todoId);

        todoService.deleteTodo(userIdString, todoString);

        verify(todoRepository).deleteTodo(todoId);
    }

    @Test
    void deleteTodoErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.deleteTodo(userIdString, todoString)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verifyNoInteractions(todoRepository);
    }

    @Test
    void deleteTodoErrorTodoNotFound() {
        UUID userId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();
        String todoString = todoId.toString();
        String userIdString = userId.toString();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTodoId(userId, todoId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.deleteTodo(userIdString, todoString)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "Todo not found");
        verify(todoRepository, never()).deleteTodo(any(UUID.class));
    }

    private CreateTodoRequest createTodoRequest() {
        return new CreateTodoRequest(TITLE, DESCRIPTION);
    }

    private Todo saveTodo(UUID userId) {
        return  Todo.builder()
                .todoId(UUID.randomUUID())
                .userId(userId)
                .title(TITLE)
                .description(DESCRIPTION)
                .status(TodoStatus.PENDING)
                .createdAt(Date.from(NOW))
                .updatedAt(Date.from(NOW))
                .build();
    }

    private Todo createTodo(
            UUID todoId,
            UUID userId,
            String title,
            String description
    ) {
        return Todo.builder()
                .todoId(todoId)
                .userId(userId)
                .title(title)
                .description(description)
                .status(TodoStatus.PENDING)
                .createdAt(Date.from(NOW))
                .updatedAt(Date.from(NOW))
                .build();
    }

    private void mockExistingUserAndOwnedTodo(UUID userId, UUID todoId) {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(todoRepository.existByUserIdAndTodoId(userId, todoId)).thenReturn(true);
    }

    private void assertTodoResponse(TodoResponse response, Todo todo) {
        assertAll(
                () -> assertEquals(todo.getTodoId(), response.todoId()),
                () -> assertEquals(todo.getTitle(), response.title()),
                () -> assertEquals(todo.getDescription(), response.description()),
                () -> assertEquals(todo.getStatus(), response.status()),
                () -> assertEquals(todo.getCreatedAt(), response.createdAt()),
                () -> assertEquals(todo.getUpdatedAt(), response.updatedAt())
        );
    }

    private void assertStatusException(
            ResponseStatusException exception,
            HttpStatus expectedStatus,
            String expectedReason
    ) {
        assertAll(
                () -> assertEquals(expectedStatus, exception.getStatusCode()),
                () -> assertEquals(expectedReason, exception.getReason())
        );
    }
}
