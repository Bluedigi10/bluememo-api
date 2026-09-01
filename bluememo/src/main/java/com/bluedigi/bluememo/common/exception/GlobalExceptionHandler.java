package com.bluedigi.bluememo.common.exception;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error processing request: {}",
                request.getRequestURI(),
                exception
        );
        return buildResponse(
                "An unexpected error occurred while processing the request",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(BluememoException.class)
    public ResponseEntity<ErrorResponse> handleBluememoException(
            BluememoException exception,
            HttpServletRequest request
    ) {
        log.error(
                "Application error processing request: {}",
                request.getRequestURI(),
                exception
        );
        return buildResponse(
                exception.getMessage(),
                HttpStatus.valueOf(exception.getStatusCode()),
                request.getRequestURI()
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation error");

        return buildResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
        ) {

            return buildResponse(
                    exception.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    request.getRequestURI()
            );

        }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());

        return buildResponse(
                exception.getReason(),
                status,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException authException,
            HttpServletRequest request
    ) {
        String errorMessage;

        if (authException instanceof BadCredentialsException && authException.getMessage() != null) {
            errorMessage = authException.getMessage();
        } else {
            errorMessage = "You need a valid token to access this resource";
        }

        return buildResponse(
                errorMessage,
                HttpStatus.UNAUTHORIZED,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException accessDeniedException,
            HttpServletRequest request
    ) {
        return buildResponse(
                "You don't have permission to access this resource",
                HttpStatus.FORBIDDEN,
                request.getRequestURI()
        );
    }


    private ResponseEntity<ErrorResponse> buildResponse(
            String message,
            HttpStatus status,
            String path
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                message,
                status.value(),
                path,
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}
