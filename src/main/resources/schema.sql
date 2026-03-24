CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME,
    token VARCHAR(255),
    failed_attempts INT NOT NULL DEFAULT 0,
    lock_until DATETIME
);