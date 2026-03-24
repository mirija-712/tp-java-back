package com.example.auth.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {
    public boolean isValid(String password) {
        if (password == null || password.length() < 12) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}