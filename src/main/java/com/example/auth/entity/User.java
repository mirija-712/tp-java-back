package com.example.auth.entity;

import jakarta.persistence.*;

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

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "token")
    private String token;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

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
     * @param passwordHash hash du mot de passe
     */
    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.failedAttempts = 0;
        this.lockUntil = null;
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
     * @return hash du mot de passe
     */
    public String getPasswordHash() { return passwordHash; }

    /**
     * @return date de création du compte
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    public int getFailedAttempts() { return failedAttempts; }

    public LocalDateTime getLockUntil() { return lockUntil; }

    public void setEmail(String email) { this.email = email; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setToken(String token) { this.token = token; }

    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
}