package com.bluedigi.bluememo.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.bluedigi.bluememo.identity.domain.model.User;
import com.bluedigi.bluememo.identity.domain.repository.UserRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.UserMapper;
import com.bluedigi.bluememo.identity.infrastructure.web.request.LoginUserRequest;
import com.bluedigi.bluememo.identity.infrastructure.web.request.RegisterUserRequest;
import com.bluedigi.bluememo.identity.infrastructure.web.response.AuthResponse;
import com.bluedigi.bluememo.shared.infrastructure.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String EMAIL = "david@example.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String TOKEN = "jwt-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUserSuccessful() {
        // Arrange
        RegisterUserRequest request = new RegisterUserRequest("David", EMAIL, RAW_PASSWORD);

        User mappedUser = createUser(null, RAW_PASSWORD);
        User savedUser = createUser(UUID.randomUUID(), ENCODED_PASSWORD);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userMapper.registerUserRequestToUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn(TOKEN);

        // Act
        AuthResponse response = authService.registerUser(request);

        // Assert
        assertEquals(TOKEN, response.token());
        assertEquals(ENCODED_PASSWORD, mappedUser.getPassword());

        verify(userRepository).existsByEmail(EMAIL);
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(mappedUser);
        verify(jwtService).generateToken(savedUser);
    }

    @Test
    void emailAlreadyExistError() {
        RegisterUserRequest request = new RegisterUserRequest("David", EMAIL, RAW_PASSWORD);

        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.registerUser(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper, passwordEncoder, jwtService);

    }

    @Test
    void loginUserSuccessful() {
        LoginUserRequest request = new LoginUserRequest(EMAIL, RAW_PASSWORD);
        User existingUser = createUser(null, ENCODED_PASSWORD);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn(TOKEN);

        AuthResponse response = authService.loginUser(request);

        assertEquals(TOKEN, response.token());

        verify(userRepository).findByEmail(EMAIL);
        verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);
        verify(jwtService).generateToken(existingUser);

        verifyNoInteractions(userMapper);

    }

    @Test
    void emailDoesntExist() {
        LoginUserRequest request = new LoginUserRequest(EMAIL, RAW_PASSWORD);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.loginUser(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid credentials", exception.getReason());

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void invalidPassword() {
        LoginUserRequest request = new LoginUserRequest(EMAIL, RAW_PASSWORD);
        User existingUser = createUser(null, ENCODED_PASSWORD);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.loginUser(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid credentials", exception.getReason());

        verifyNoInteractions(jwtService);
    }

    private User createUser(UUID id, String password) {
        User user = new User();
        user.setId(id);
        user.setName("David");
        user.setEmail(EMAIL);
        user.setPassword(password);
        return user;
    }
    
}
