CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME,
    token VARCHAR(255),
    failed_attempts INT NOT NULL DEFAULT 0,
    lock_until DATETIME
);

CREATE TABLE IF NOT EXISTS auth_nonce (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     user_id BIGINT NOT NULL,
     nonce VARCHAR(128) NOT NULL,
    expires_at DATETIME NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_auth_nonce UNIQUE (user_id, nonce),
    CONSTRAINT fk_auth_nonce_user FOREIGN KEY (user_id) REFERENCES users(id)
    );