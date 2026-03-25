package com.example.auth.service;

import com.example.auth.entity.AuthNonce;
import com.example.auth.entity.User;
import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.exception.InvalidInputException;
import com.example.auth.exception.ResourceConflictException;
import com.example.auth.exception.TooManyAttemptsException;
import com.example.auth.repository.AuthNonceRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.HmacService;
import com.example.auth.security.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthNonceRepository authNonceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private HmacService hmacService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                authNonceRepository,
                passwordEncoder,
                passwordPolicyValidator,
                hmacService
        );
    }

    @Test
    void register_shouldThrowWhenEmailInvalid() {
        assertThrows(InvalidInputException.class, () -> authService.register("", "Pwd1234!Pwd1"));
    }

    @Test
    void register_shouldThrowWhenPasswordPolicyFails() {
        when(passwordPolicyValidator.isValid("weak")).thenReturn(false);
        assertThrows(InvalidInputException.class, () -> authService.register("a@b.com", "weak"));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        when(passwordPolicyValidator.isValid("Pwd1234!Pwd1")).thenReturn(true);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));
        assertThrows(ResourceConflictException.class, () -> authService.register("a@b.com", "Pwd1234!Pwd1"));
    }

    @Test
    void register_shouldPersistHashAndClearPassword() {
        when(passwordPolicyValidator.isValid("Pwd1234!Pwd1")).thenReturn(true);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Pwd1234!Pwd1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register("a@b.com", "Pwd1234!Pwd1");

        assertEquals("hashed", saved.getPasswordHash());
        assertEquals("Pwd1234!Pwd1", saved.getPasswordClear());
    }

    @Test
    void login_shouldThrowWhenUserUnknown() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThrows(AuthenticationFailedException.class, () -> authService.login("x@y.com", "Pwd1234!Pwd1"));
    }

    @Test
    void login_shouldThrowWhenLocked() {
        User user = buildUser("x@y.com");
        user.setLockUntil(LocalDateTime.now().plusMinutes(1));
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));

        assertThrows(TooManyAttemptsException.class, () -> authService.login("x@y.com", "Pwd1234!Pwd1"));
    }

    @Test
    void login_shouldIncreaseFailedAttemptsOnWrongPassword() {
        User user = buildUser("x@y.com");
        user.setFailedAttempts(1);
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> authService.login("x@y.com", "wrong"));
        assertEquals(2, user.getFailedAttempts());
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    void login_shouldSetLockAfterFiveFailures() {
        User user = buildUser("x@y.com");
        user.setFailedAttempts(4);
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> authService.login("x@y.com", "wrong"));
        assertEquals(0, user.getFailedAttempts());
        assertNotNull(user.getLockUntil());
    }

    @Test
    void login_shouldResetFlagsAndCreateTokenWhenSuccess() {
        User user = buildUser("x@y.com");
        user.setFailedAttempts(3);
        user.setLockUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User logged = authService.login("x@y.com", "ok");
        assertEquals(0, logged.getFailedAttempts());
        assertNull(logged.getLockUntil());
        assertNotNull(logged.getToken());
        assertNotNull(logged.getTokenExpiresAt());
    }

    @Test
    void loginWithProof_shouldRejectReplayNonce() {
        User user = buildUser("x@y.com");
        long now = Instant.now().getEpochSecond();
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));
        when(authNonceRepository.findByUserAndNonce(user, "n1")).thenReturn(Optional.of(new AuthNonce()));

        assertThrows(AuthenticationFailedException.class,
                () -> authService.loginWithProof("x@y.com", "n1", now, "abcd"));
    }

    @Test
    void loginWithProof_shouldRegisterFailureWhenHmacInvalid() {
        User user = buildUser("x@y.com");
        long now = Instant.now().getEpochSecond();
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));
        when(authNonceRepository.findByUserAndNonce(user, "n1")).thenReturn(Optional.empty());
        when(hmacService.hmacSha256Hex("secret", "x@y.com:n1:" + now)).thenReturn("expected");
        when(hmacService.constantTimeEqualsHex("expected", "invalid")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class,
                () -> authService.loginWithProof("x@y.com", "n1", now, "invalid"));
        assertEquals(1, user.getFailedAttempts());
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    void loginWithProof_shouldSucceedAndConsumeNonceWhenValid() {
        User user = buildUser("x@y.com");
        long now = Instant.now().getEpochSecond();
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(user));
        when(authNonceRepository.findByUserAndNonce(user, "n2")).thenReturn(Optional.empty());
        when(hmacService.hmacSha256Hex("secret", "x@y.com:n2:" + now)).thenReturn("expected");
        when(hmacService.constantTimeEqualsHex("expected", "expected")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User logged = authService.loginWithProof("x@y.com", "n2", now, "expected");
        assertNotNull(logged.getToken());
        assertNotNull(logged.getTokenExpiresAt());

        ArgumentCaptor<AuthNonce> nonceCaptor = ArgumentCaptor.forClass(AuthNonce.class);
        verify(authNonceRepository, atLeast(2)).save(nonceCaptor.capture());
        assertTrue(nonceCaptor.getAllValues().get(nonceCaptor.getAllValues().size() - 1).isConsumed());
    }

    @Test
    void getUserByToken_shouldThrowWhenTokenInvalid() {
        when(userRepository.findByTokenAndTokenExpiresAtAfter(eq("bad"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        assertThrows(AuthenticationFailedException.class, () -> authService.getUserByToken("bad"));
    }

    private User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setPasswordClear("secret");
        user.setCreatedAt(LocalDateTime.now());
        user.setFailedAttempts(0);
        return user;
    }
}
