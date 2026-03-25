CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_clear VARCHAR(255),
    created_at DATETIME,
    token VARCHAR(255),
    token_expires_at DATETIME,
    failed_attempts INT NOT NULL DEFAULT 0,
    lock_until DATETIME
);

-- Migration incrémentale TP3 pour bases déjà existantes
SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'password_clear'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN password_clear VARCHAR(255)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'token_expires_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN token_expires_at DATETIME',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'failed_attempts'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'lock_until'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN lock_until DATETIME',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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