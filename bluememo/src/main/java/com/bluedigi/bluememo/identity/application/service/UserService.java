package com.bluedigi.bluememo.identity.application.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bluedigi.bluememo.identity.domain.model.User;
import com.bluedigi.bluememo.identity.domain.repository.ChannelAccountRepository;
import com.bluedigi.bluememo.identity.domain.repository.ChannelLinkTokenRepository;
import com.bluedigi.bluememo.identity.domain.repository.UserRepository;
import com.bluedigi.bluememo.identity.infrastructure.persistence.mapper.UserMapper;
import com.bluedigi.bluememo.identity.infrastructure.web.request.UpdateUserRequest;
import com.bluedigi.bluememo.identity.infrastructure.web.request.DeleteUserRequest;
import com.bluedigi.bluememo.identity.infrastructure.web.response.UserResponse;
import com.bluedigi.bluememo.todo.domain.repository.TodoRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ChannelAccountRepository channelAccountRepository;
    private final ChannelLinkTokenRepository channelLinkTokenRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return userMapper.userToUserResponse(user);
    }

    @Transactional
    public void deleteUserById(String userId, DeleteUserRequest deleteUserRequest) {
        if (!channelAccountRepository.lockUser(UUID.fromString(userId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(deleteUserRequest.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        deleteChannelAccountsByUserId(UUID.fromString(userId));
        todoRepository.deleteTodosByUserId(UUID.fromString(userId));
        userRepository.deleteById(UUID.fromString(userId));
    }

    @Transactional
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
                );

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (request.email() != null
                && !request.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        if (request.phone() != null
                && !request.phone().equalsIgnoreCase(user.getPhone())
                && userRepository.existsByPhone(request.phone())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phone already exists"
            );
        }

        if (request.email() != null) {
            user.setEmail(request.email());
        }

        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        if (request.birthdate() != null) {
            user.setBirthdate(request.birthdate());
        }

        if (request.newPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        User updatedUser = userRepository.update(user);

        return userMapper.userToUserResponse(updatedUser);
    }

    private void deleteChannelAccountsByUserId(UUID userId) {
        channelAccountRepository.deleteByUserId(userId);
        channelLinkTokenRepository.deleteByUserId(userId);
    }
}
