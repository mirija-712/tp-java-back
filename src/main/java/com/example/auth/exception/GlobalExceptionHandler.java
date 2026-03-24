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

    /**
     * Gère les erreurs de validation et de saisie.
     *
     * @param ex exception métier
     * @param request requête HTTP courante
     * @return réponse 400 structurée
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<?> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "path", request.getRequestURI()
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
    public ResponseEntity<?> handleAuthFailed(AuthenticationFailedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "message", ex.getMessage(),
                "path", request.getRequestURI()
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
    public ResponseEntity<?> handleConflict(ResourceConflictException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "path", request.getRequestURI()
        ));
    }
}