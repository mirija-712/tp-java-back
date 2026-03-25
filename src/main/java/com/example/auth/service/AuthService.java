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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service principal d'authentification.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final AuthNonceRepository authNonceRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final HmacService hmacService;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 2;
    private static final long TIMESTAMP_WINDOW_SECONDS = 60;
    private static final long NONCE_TTL_SECONDS = 120;
    private static final long TOKEN_TTL_MINUTES = 15;

    /**
     * Construit le service d'authentification.
     *
     * @param userRepository repository des utilisateurs
     */
    public AuthService(UserRepository userRepository,
                       AuthNonceRepository authNonceRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyValidator passwordPolicyValidator,
                       HmacService hmacService) {
        this.userRepository = userRepository;
        this.authNonceRepository = authNonceRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.hmacService = hmacService;
    }


    /**
     * Inscrit un nouvel utilisateur après validation des données.
     *
     * @param email email utilisateur
     * @param password mot de passe en clair (TP volontairement non sécurisé)
     * @return utilisateur créé
     * @throws InvalidInputException si email ou mot de passe est invalide
     * @throws ResourceConflictException si l'email existe déjà
     */
    public User register(String email, String password) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            logger.warn("Inscription échouée : email invalide");
            throw new InvalidInputException("Email invalide");
        }
        if (!passwordPolicyValidator.isValid(password)) {
            logger.warn("Inscription échouée : mot de passe non conforme à la politique TP2");
            throw new InvalidInputException("Mot de passe non conforme à la politique TP2");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Inscription échouée : email déjà existant {}", email);
            throw new ResourceConflictException("Email déjà utilisé");
        }

        User user = new User(email, passwordEncoder.encode(password));
        user.setPasswordClear(password);
        userRepository.save(user);
        logger.info("Inscription réussie pour {}", email);
        return user;
    }

    /**
     * Connecte un utilisateur et génère un token de session.
     *
     * @param email email utilisateur
     * @param password mot de passe en clair
     * @return utilisateur authentifié avec token mis à jour
     * @throws AuthenticationFailedException si les identifiants sont incorrects
     */
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Email ou mot de passe incorrect"));

        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new TooManyAttemptsException("Compte temporairement bloque");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int nextAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(nextAttempts);
            if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setFailedAttempts(0);
            }
            userRepository.save(user);
            throw new AuthenticationFailedException("Email ou mot de passe incorrect");
        }

        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setToken(UUID.randomUUID().toString());
        user.setTokenExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        return userRepository.save(user);
    }

    /**
     * Login TP3 par preuve HMAC (email + nonce + timestamp).
     */
    public User loginWithProof(String email, String nonce, long timestamp, String hmacHex) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("TP3 login refusé: utilisateur introuvable pour {}", email);
                    return new AuthenticationFailedException("Accès refusé");
                });

        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            logger.warn("TP3 login refusé: compte verrouillé pour {}", email);
            throw new TooManyAttemptsException("Compte temporairement bloque");
        }
        if (nonce == null || nonce.isBlank()) {
            logger.warn("TP3 login refusé: nonce vide pour {}", email);
            throw new AuthenticationFailedException("Accès refusé");
        }

        // Accepte secondes (TP3 attendu) et millisecondes (erreur front fréquente).
        long timestampSeconds = timestamp > 10_000_000_000L ? (timestamp / 1000L) : timestamp;
        long nowEpoch = Instant.now().getEpochSecond();
        if (Math.abs(nowEpoch - timestampSeconds) > TIMESTAMP_WINDOW_SECONDS) {
            logger.warn("TP3 login refusé: timestamp hors fenêtre pour {} (reçu={}, normalisé={})",
                    email, timestamp, timestampSeconds);
            throw new AuthenticationFailedException("Accès refusé");
        }

        authNonceRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (authNonceRepository.findByUserAndNonce(user, nonce).isPresent()) {
            logger.warn("TP3 login refusé: nonce rejoué pour {} nonce={}", email, nonce);
            throw new AuthenticationFailedException("Accès refusé");
        }

        AuthNonce savedNonce = new AuthNonce();
        savedNonce.setUser(user);
        savedNonce.setNonce(nonce);
        savedNonce.setCreatedAt(LocalDateTime.now());
        savedNonce.setExpiresAt(LocalDateTime.now().plusSeconds(NONCE_TTL_SECONDS));
        savedNonce.setConsumed(false);
        authNonceRepository.save(savedNonce);
        logger.info("TP3 nonce enregistré pour {} nonce={}", email, nonce);

        String clearSecret = user.getPasswordClear();
        if (clearSecret == null || clearSecret.isBlank()) {
            logger.warn("TP3 login refusé: password_clear absent pour {}", email);
            throw new AuthenticationFailedException("Accès refusé");
        }
        String message = email + ":" + nonce + ":" + timestampSeconds;
        String expected = hmacService.hmacSha256Hex(clearSecret, message);
        if (!hmacService.constantTimeEqualsHex(expected, hmacHex)) {
            logger.warn("TP3 login refusé: hmac invalide pour {}", email);
            registerFailedAttempt(user);
            throw new AuthenticationFailedException("Accès refusé");
        }

        savedNonce.setConsumed(true);
        authNonceRepository.save(savedNonce);

        user.setFailedAttempts(0);
        user.setLockUntil(null);
        user.setToken(UUID.randomUUID().toString());
        user.setTokenExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        return userRepository.save(user);
    }

    /**
     * Récupère un utilisateur via son token.
     *
     * @param token token d'authentification
     * @return utilisateur correspondant
     * @throws AuthenticationFailedException si le token est invalide
     */
    public User getUserByToken(String token) {
        return userRepository.findByTokenAndTokenExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new AuthenticationFailedException("Token invalide"));
    }

    private void registerFailedAttempt(User user) {
        int nextAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(nextAttempts);
        if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setFailedAttempts(0);
        }
        userRepository.save(user);
    }
}