package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
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

    /**
     * -- GETTER --
     *
     * @return identifiant technique
     */
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * -- GETTER --
     *
     * @return email utilisateur
     */
    @Getter
    @Setter
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * -- GETTER --
     *
     * @return mot de passe en clair (TP non sécurisé)
     */
    // TP1 volontairement dangereux : mot de passe en clair
    @Getter
    @Setter
    @Column(name = "password_clear", nullable = false)
    private String password;

    /**
     * -- GETTER --
     *
     * @return date de création du compte
     */
    @Getter
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * -- GETTER --
     *
     * @return token d'authentification courant
     */
    @Getter
    @Setter
    @Column(name = "token")
    private String token;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

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

}