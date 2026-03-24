package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.exception.*;
import com.example.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service principal d'authentification.
 * ATTENTION : Cette implémentation est volontairement dangereuse
 * et ne doit jamais être utilisée en production.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Inscrit un nouvel utilisateur.
     */
    public User register(String email, String password) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            logger.warn("Inscription échouée : email invalide");
            throw new InvalidInputException("Email invalide");
        }
        if (password == null || password.length() < 4) {
            logger.warn("Inscription échouée : mot de passe trop court");
            throw new InvalidInputException("Mot de passe trop court (minimum 4 caractères)");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Inscription échouée : email déjà existant {}", email);
            throw new ResourceConflictException("Email déjà utilisé");
        }

        User user = new User(email, password);
        userRepository.save(user);
        logger.info("Inscription réussie pour {}", email);
        return user;
    }

    /**
     * Connecte un utilisateur existant.
     */
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Connexion échouée : email inconnu {}", email);
                    return new AuthenticationFailedException("Email ou mot de passe incorrect");
                });

        if (!user.getPassword().equals(password)) {
            logger.warn("Connexion échouée : mot de passe incorrect pour {}", email);
            throw new AuthenticationFailedException("Email ou mot de passe incorrect");
        }

        logger.info("Connexion réussie pour {}", email);
        return user;
    }

}