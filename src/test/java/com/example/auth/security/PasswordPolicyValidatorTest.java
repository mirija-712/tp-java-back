package com.example.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void isValid_shouldRejectNull() {
        assertFalse(validator.isValid(null));
    }

    @Test
    void isValid_shouldRejectShortPassword() {
        assertFalse(validator.isValid("Ab1!short"));
    }

    @Test
    void isValid_shouldRejectWithoutUppercase() {
        assertFalse(validator.isValid("abcd1234!xyz"));
    }

    @Test
    void isValid_shouldRejectWithoutDigit() {
        assertFalse(validator.isValid("Abcdefgh!ijk"));
    }

    @Test
    void isValid_shouldAcceptStrongPassword() {
        assertTrue(validator.isValid("Pwd1234!Pwd1"));
    }
}
