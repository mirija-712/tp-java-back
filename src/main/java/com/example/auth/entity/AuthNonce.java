package com.example.auth.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
    @Table(name = "auth_nonce", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "nonce"}))
    public class AuthNonce {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(nullable = false, length = 128)
        private String nonce;

        @Column(name = "expires_at", nullable = false)
        private LocalDateTime expiresAt;

        @Column(nullable = false)
        private boolean consumed;

        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;
    }

