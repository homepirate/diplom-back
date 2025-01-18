package com.example.diplom.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.AuthenticationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка ошибок валидации, возникающих при неверных входных данных.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StatusResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));

        StatusResponse response = new StatusResponse("BAD_REQUEST", errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка ошибок валидации параметров запроса.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StatusResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(constraint -> constraint.getMessage())
                .collect(Collectors.joining("; "));

        StatusResponse response = new StatusResponse("BAD_REQUEST", errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка исключений аутентификации.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StatusResponse> handleAuthenticationException(AuthenticationException ex) {
        StatusResponse response = new StatusResponse("UNAUTHORIZED", "Authentication failed");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Обработка исключений, связанных с нарушением целостности данных (например, уникальные ограничения).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StatusResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String errorMessage = "Data integrity violation";

        // Проверяем, является ли причина исключения ConstraintViolationException от Hibernate
            // Альтернативный способ: анализировать сообщение исключения
            String message = ex.getMessage();
            if (message != null && (message.contains("users_phone_key") || message.contains("users_email_key"))) {
                errorMessage = "Phone number or email already taken.";

        }

        StatusResponse response = new StatusResponse("BAD_REQUEST", errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StatusResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        StatusResponse response = new StatusResponse("NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
        /**
         * Обработка любых других непредвиденных ошибок.
         */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StatusResponse> handleAllExceptions(Exception ex) {
        StatusResponse response = new StatusResponse("INTERNAL_SERVER_ERROR", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
