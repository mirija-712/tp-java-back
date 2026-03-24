package com.example.auth.service;

import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.exception.InvalidInputException;
import com.example.auth.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testInscriptionOK() {
        assertDoesNotThrow(() -> authService.register("test@example.com", "abcd"));
    }

    @Test
    void testInscriptionEmailDejaExistant() {
        authService.register("double@example.com", "abcd");
        assertThrows(ResourceConflictException.class, () ->
                authService.register("double@example.com", "abcd"));
    }

    @Test
    void testInscriptionEmailVide() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("", "abcd"));
    }

    @Test
    void testInscriptionEmailFormatInvalide() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("pasunemail", "abcd"));
    }

    @Test
    void testInscriptionMotDePasseTropCourt() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("court@example.com", "abc"));
    }

    @Test
    void testConnexionOK() {
        authService.register("login@example.com", "abcd");
        assertDoesNotThrow(() -> authService.login("login@example.com", "abcd"));
    }

    @Test
    void testConnexionMotDePasseIncorrect() {
        authService.register("wrong@example.com", "abcd");
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login("wrong@example.com", "mauvais"));
    }

    @Test
    void testConnexionEmailInconnu() {
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login("inconnu@example.com", "abcd"));
    }
}