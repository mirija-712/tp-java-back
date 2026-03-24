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

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthServiceTest {
    private static final String STRONG_PASSWORD = "Pwd1234!Pwd1";

    @Autowired
    private AuthService authService;

    @Test
    void testInscriptionOK() {
        assertDoesNotThrow(() -> authService.register("test@example.com", STRONG_PASSWORD));
    }

    @Test
    void testInscriptionEmailDejaExistant() {
        authService.register("double@example.com", STRONG_PASSWORD);
        assertThrows(ResourceConflictException.class, () ->
                authService.register("double@example.com", STRONG_PASSWORD));
    }

    @Test
    void testInscriptionEmailVide() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("", STRONG_PASSWORD));
    }

    @Test
    void testInscriptionEmailFormatInvalide() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("pasunemail", STRONG_PASSWORD));
    }

    @Test
    void testInscriptionMotDePasseTropCourt() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("court@example.com", "Ab1!"));
    }

    @Test
    void testConnexionOK() {
        authService.register("login@example.com", STRONG_PASSWORD);
        assertDoesNotThrow(() -> authService.login("login@example.com", STRONG_PASSWORD));
    }

    @Test
    void testConnexionMotDePasseIncorrect() {
        authService.register("wrong@example.com", STRONG_PASSWORD);
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login("wrong@example.com", "mauvais"));
    }

    @Test
    void testConnexionEmailInconnu() {
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login("inconnu@example.com", STRONG_PASSWORD));
    }
}