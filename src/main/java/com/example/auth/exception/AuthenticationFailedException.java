package com.example.auth.exception;

/**
 * Exception levée quand l'authentification échoue.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}