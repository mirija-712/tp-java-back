package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité représentant un utilisateur.
 * ATTENTION : Cette implémentation est volontairement da   ngereuse
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

    public String getToken() { return token; }

    public User() {}

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public String getEmail() { return email; }

    public String getPassword() { return password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}