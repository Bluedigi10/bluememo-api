package com.bluedigi.bluememo.identity.application.service;

import com.bluedigi.bluememo.identity.domain.model.User;
import com.bluedigi.bluememo.identity.domain.repository.UserRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.UserMapper;
import com.bluedigi.bluememo.identity.infrastructure.web.request.DeleteUserRequest;
import com.bluedigi.bluememo.identity.infrastructure.web.request.UpdateUserRequest;
import com.bluedigi.bluememo.identity.infrastructure.web.response.UserResponse;
import com.bluedigi.bluememo.todo.domain.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.bluedigi.bluememo.identity.domain.repository.ChannelAccountRepository;
import com.bluedigi.bluememo.identity.domain.repository.ChannelLinkTokenRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String NAME = "David";
    private static final String EMAIL = "david@example.com";
    private static final String PHONE = "5512345678";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String NEW_RAW_PASSWORD = "new-password123";
    private static final String NEW_ENCODED_PASSWORD = "new-encoded-password";
    private static final LocalDate BIRTHDATE = LocalDate.of(2000, 1, 15);
    private static final Date CREATED_AT = Date.from(Instant.parse("2026-08-01T12:00:00Z"));
    private static final Date UPDATED_AT = Date.from(Instant.parse("2026-08-02T12:00:00Z"));

    @Mock
    private UserRepository userRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private ChannelAccountRepository channelAccountRepository;

    @Mock
    private ChannelLinkTokenRepository channelLinkTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final UserMapper userMapper = new UserMapper();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                todoRepository,
                userMapper,
                passwordEncoder,
                channelAccountRepository,
                channelLinkTokenRepository
        );
    }

    @Test
    void getUserByIdSuccess() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        User user = createUser(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(userIdString);

        assertAll(
                () -> assertEquals(NAME, response.name()),
                () -> assertEquals(EMAIL, response.email()),
                () -> assertEquals(PHONE, response.phone()),
                () -> assertEquals(BIRTHDATE, response.birthdate()),
                () -> assertEquals(CREATED_AT, response.createdAt()),
                () -> assertEquals(UPDATED_AT, response.updatedAt())
        );

        verify(userRepository).findById(userId);
        verifyNoInteractions(todoRepository, passwordEncoder);
    }

    @Test
    void getUserByIdErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getUserById(userIdString)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verifyNoInteractions(todoRepository, passwordEncoder);
    }

    @Test
    void deleteUserByIdSuccess() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        User user = createUser(userId);
        DeleteUserRequest request = new DeleteUserRequest(RAW_PASSWORD);

        when(channelAccountRepository.lockUser(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        userService.deleteUserById(userIdString, request);

        verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);

        InOrder deletionOrder = inOrder(channelAccountRepository, channelLinkTokenRepository, todoRepository, userRepository);
        deletionOrder.verify(channelAccountRepository).deleteByUserId(userId);
        deletionOrder.verify(channelLinkTokenRepository).deleteByUserId(userId);
        deletionOrder.verify(todoRepository).deleteTodosByUserId(userId);
        deletionOrder.verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUserByIdErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        DeleteUserRequest request = new DeleteUserRequest(RAW_PASSWORD);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUserById(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verify(userRepository, never()).deleteById(any(UUID.class));
        verifyNoInteractions(todoRepository, passwordEncoder);
    }

    @Test
    void deleteUserByIdErrorPasswordIncorrect() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String userIdString = userId.toString();
        DeleteUserRequest request = new DeleteUserRequest("incorrect-password");

        when(channelAccountRepository.lockUser(userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), ENCODED_PASSWORD)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.deleteUserById(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.UNAUTHORIZED, "Invalid password");
        verify(userRepository, never()).deleteById(any(UUID.class));
        verifyNoInteractions(todoRepository, channelLinkTokenRepository);
        verify(channelAccountRepository, never()).deleteByUserId(any());
    }

    @Test
    void updateUserAllFields() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String userIdString = userId.toString();
        LocalDate newBirthdate = LocalDate.of(1999, 5, 20);
        UpdateUserRequest request = new UpdateUserRequest(
                "David Martinez",
                "new-email@example.com",
                "5587654321",
                newBirthdate,
                RAW_PASSWORD,
                NEW_RAW_PASSWORD
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhone(request.phone())).thenReturn(false);
        when(passwordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        when(userRepository.update(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(userIdString, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(userCaptor.capture());
        User updatedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals(request.name(), updatedUser.getName()),
                () -> assertEquals(request.email(), updatedUser.getEmail()),
                () -> assertEquals(request.phone(), updatedUser.getPhone()),
                () -> assertEquals(newBirthdate, updatedUser.getBirthdate()),
                () -> assertEquals(NEW_ENCODED_PASSWORD, updatedUser.getPassword()),
                () -> assertEquals(request.name(), response.name()),
                () -> assertEquals(request.email(), response.email()),
                () -> assertEquals(request.phone(), response.phone()),
                () -> assertEquals(newBirthdate, response.birthdate())
        );

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).existsByPhone(request.phone());
        verify(passwordEncoder).encode(NEW_RAW_PASSWORD);
    }

    @Test
    void updateUserOnlyNameUpdated() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String userIdString = userId.toString();
        UpdateUserRequest request = new UpdateUserRequest(
                "David Martinez",
                null,
                null,
                null,
                RAW_PASSWORD,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.update(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(userIdString, request);

        assertAll(
                () -> assertEquals(request.name(), response.name()),
                () -> assertEquals(EMAIL, response.email()),
                () -> assertEquals(PHONE, response.phone()),
                () -> assertEquals(BIRTHDATE, response.birthdate()),
                () -> assertEquals(ENCODED_PASSWORD, user.getPassword())
        );

        verify(userRepository, never()).existsByEmail(any(String.class));
        verify(userRepository, never()).existsByPhone(any(String.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void updateUserPhoneAndEmailAreTheSame() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        User user = createUser(userId);
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                EMAIL.toUpperCase(),
                PHONE,
                null,
                RAW_PASSWORD,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.update(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(userIdString, request);

        assertAll(
                () -> assertEquals(EMAIL.toUpperCase(), response.email()),
                () -> assertEquals(PHONE, response.phone())
        );

        verify(userRepository, never()).existsByEmail(any(String.class));
        verify(userRepository, never()).existsByPhone(any(String.class));
    }

    @Test
    void updateUserErrorUserNotFound() {
        UUID userId = UUID.randomUUID();
        String userIdString = userId.toString();
        UpdateUserRequest request = updateNameRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.NOT_FOUND, "User not found");
        verify(userRepository, never()).update(any(User.class));
        verifyNoInteractions(todoRepository, passwordEncoder);
    }

    @Test
    void updateUserErrorPasswordIncorrect() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String userIdString = userId.toString();
        UpdateUserRequest request = updateNameRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.UNAUTHORIZED, "Invalid credentials");
        verify(userRepository, never()).update(any(User.class));
    }

    @Test
    void updateUserErrorEmailAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String userIdString = userId.toString();
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                "existing@example.com",
                null,
                null,
                RAW_PASSWORD,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.CONFLICT, "Email already exists");
        verify(userRepository, never()).update(any(User.class));
        verify(userRepository, never()).existsByPhone(any(String.class));
    }

    @Test
    void updateUserErrorPhoneAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String userIdString = userId.toString();
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                null,
                "5599999999",
                null,
                RAW_PASSWORD,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.existsByPhone(request.phone())).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(userIdString, request)
        );

        assertStatusException(exception, HttpStatus.CONFLICT, "Phone already exists");
        verify(userRepository, never()).update(any(User.class));
    }

    private User createUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setName(NAME);
        user.setEmail(EMAIL);
        user.setPhone(PHONE);
        user.setPassword(ENCODED_PASSWORD);
        user.setBirthdate(BIRTHDATE);
        user.setCreatedAt(CREATED_AT);
        user.setUpdatedAt(UPDATED_AT);
        return user;
    }

    private UpdateUserRequest updateNameRequest() {
        return new UpdateUserRequest(
                "David Martinez",
                null,
                null,
                null,
                RAW_PASSWORD,
                null
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
