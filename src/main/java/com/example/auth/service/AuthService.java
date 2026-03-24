package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.exception.InvalidInputException;
import com.example.auth.exception.ResourceConflictException;
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

    /**
     * Construit le service d'authentification.
     *
     * @param userRepository repository des utilisateurs
     */
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
     * Connecte un utilisateur et génère un token de session.
     *
     * @param email email utilisateur
     * @param password mot de passe en clair
     * @return utilisateur authentifié avec token mis à jour
     * @throws AuthenticationFailedException si les identifiants sont incorrects
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

        // Génère un token simple et le sauvegarde
        String token = java.util.UUID.randomUUID().toString();
        user.setToken(token);
        userRepository.save(user);

        logger.info("Connexion réussie pour {}", email);
        return user;
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