package com.example.auth.dto;

/**
 * DTO pour la requête de connexion.
 * ATTENTION : Cette implémentation est volontairement dangereuse
 * et ne doit jamais être utilisée en production.
 */

public class LoginRequest {
    private String email;
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
