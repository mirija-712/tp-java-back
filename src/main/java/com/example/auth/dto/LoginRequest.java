package com.example.auth.dto;

/**
 * DTO pour la requête de connexion.
 * ATTENTION : Cette implémentation est volontairement dangereuse
 * et ne doit jamais être utilisée en production.
 */

public class LoginRequest {
    private String email;
    private String password;
    private String nonce;
    private long timestamp;
    private String hmac;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getHmac() { return hmac; }
    public void setHmac(String hmac) { this.hmac = hmac; }
}


