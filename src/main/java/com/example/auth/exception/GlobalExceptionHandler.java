package com.example.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Gestion centralisée des exceptions métier HTTP.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_PATH = "path";

    /**
     * Gère les erreurs de validation et de saisie.
     *
     * @param ex exception métier
     * @param request requête HTTP courante
     * @return réponse 400 structurée
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                KEY_TIMESTAMP, LocalDateTime.now().toString(),
                KEY_STATUS, HttpStatus.BAD_REQUEST.value(),
                KEY_ERROR, HttpStatus.BAD_REQUEST.getReasonPhrase(),
                KEY_MESSAGE, ex.getMessage(),
                KEY_PATH, request.getRequestURI()
        ));
    }

    /**
     * Gère les erreurs d'authentification.
     *
     * @param ex exception métier
     * @param request requête HTTP courante
     * @return réponse 401 structurée
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthFailed(AuthenticationFailedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                KEY_TIMESTAMP, LocalDateTime.now().toString(),
                KEY_STATUS, HttpStatus.UNAUTHORIZED.value(),
                KEY_ERROR, HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                KEY_MESSAGE, ex.getMessage(),
                KEY_PATH, request.getRequestURI()
        ));
    }

    /**
     * Gère les conflits de ressource (doublons).
     *
     * @param ex exception métier
     * @param request requête HTTP courante
     * @return réponse 409 structurée
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ResourceConflictException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                KEY_TIMESTAMP, LocalDateTime.now().toString(),
                KEY_STATUS, HttpStatus.CONFLICT.value(),
                KEY_ERROR, HttpStatus.CONFLICT.getReasonPhrase(),
                KEY_MESSAGE, ex.getMessage(),
                KEY_PATH, request.getRequestURI()
        ));
    }
}