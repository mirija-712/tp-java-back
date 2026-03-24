package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.exception.InvalidInputException;
import com.example.auth.exception.ResourceConflictException;
import com.example.auth.exception.TooManyAttemptsException;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service principal d'authentification.
 * ATTENTION : Cette implémentation est volontairement dangereuse
 * et ne doit jamais être utilisée en production.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 2;

    /**
     * Construit le service d'authentification.
     *
     * @param userRepository repository des utilisateurs
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordPolicyValidator passwordPolicyValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
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
        return userRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationFailedException("Token invalide"));
    }

}