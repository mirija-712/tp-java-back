-- Hash BCrypt de "Pwd1234!Pwd1" (exemple TP2)
INSERT IGNORE INTO users (email, password_hash, password_clear, created_at, token, token_expires_at, failed_attempts, lock_until)
VALUES (
    'toto@example.com',
    '$2a$10$4z8Tw4p4jQ2hQaJqjBhUL.1Lq6Ez6Jk4LFh0QWfLQtiWg3oKoD7bS',
    'Pwd1234!Pwd1',
    NOW(),
    NULL,
    NULL,
    0,
    NULL
);

INSERT IGNORE INTO users (email, password_hash, password_clear, created_at, token, token_expires_at, failed_attempts, lock_until)
VALUES (
    'steve@gmail.com',
    '$2a$10$4z8Tw4p4jQ2hQaJqjBhUL.1Lq6Ez6Jk4LFh0QWfLQtiWg3oKoD7bS',
    'Pwd1234!Pwd1',
    NOW(),
    NULL,
    NULL,
    0,
    NULL
);