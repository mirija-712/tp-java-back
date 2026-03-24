-- Hash BCrypt de "Pwd1234!Pwd1" (exemple TP2)
INSERT IGNORE INTO users (email, password_hash, created_at, token, failed_attempts, lock_until)
VALUES (
    'toto@example.com',
    '$2a$10$4z8Tw4p4jQ2hQaJqjBhUL.1Lq6Ez6Jk4LFh0QWfLQtiWg3oKoD7bS',
    NOW(),
    NULL,
    0,
    NULL
);