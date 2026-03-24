package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité représentant un utilisateur.
 * ATTENTION : Cette implémentation est volontairement dangereuse
 * et ne doit jamais être utilisée en production.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(unique = true, nullable = false)
    private String email;

    // TP1 volontairement dangereux : mot de passe en clair
    @Setter
    @Column(name = "password_clear", nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "token")
    private String token;

    /**
     * @return token d'authentification courant
     */
    public String getToken() { return token; }

    /**
     * Constructeur par défaut requis par JPA.
     */
    public User() {}

    /**
     * Construit un utilisateur métier.
     *
     * @param email email utilisateur
     * @param password mot de passe en clair
     */
    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * @return identifiant technique
     */
    public Long getId() { return id; }

    /**
     * @return email utilisateur
     */
    public String getEmail() { return email; }

    /**
     * @return mot de passe en clair (TP non sécurisé)
     */
    public String getPassword() { return password; }

    /**
     * @return date de création du compte
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
}