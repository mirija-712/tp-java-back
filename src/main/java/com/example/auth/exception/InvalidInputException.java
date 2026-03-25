package com.example.auth.exception;

/**
 * Exception levée quand les données envoyées sont invalides.
 */
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}