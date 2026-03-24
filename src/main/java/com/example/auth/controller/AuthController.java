package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.entity.User;
import com.example.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST pour l'authentification.
 * ATTENTION : Cette implémentation est volontairement dangereuse
 * et ne doit jamais être utilisée en production.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Construit le contrôleur d'authentification.
     *
     * @param authService service métier d'authentification
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint d'inscription.
     *
     * @param request corps de requête d'inscription
     * @return message de confirmation
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Inscription réussie");
    }

    /**
     * Endpoint de connexion.
     *
     * @param request corps de requête de connexion
     * @return token d'authentification
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(java.util.Map.of("token", user.getToken()));
    }

    /**
     * Retourne les informations de l'utilisateur connecté.
     *
     * @param token token fourni via l'en-tête Authorization
     * @return informations de l'utilisateur
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String token) {
        User user = authService.getUserByToken(token);
        return ResponseEntity.ok(java.util.Map.of(
                "email", user.getEmail(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }

}