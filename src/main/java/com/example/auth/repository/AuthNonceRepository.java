package com.example.auth.repository;

import com.example.auth.entity.AuthNonce;
import com.example.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthNonceRepository extends JpaRepository<AuthNonce, Long> {
    Optional<AuthNonce> findByUserAndNonce(User user, String nonce);
    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
