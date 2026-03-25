package com.example.auth.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class HmacService {
    public String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signed = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signed);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Erreur calcul HMAC", e);
        }
    }

    public boolean constantTimeEqualsHex(String leftHex, String rightHex) {
        if (leftHex == null || rightHex == null) {
            return false;
        }
        try {
            byte[] left = HexFormat.of().parseHex(leftHex);
            byte[] right = HexFormat.of().parseHex(rightHex);
            return MessageDigest.isEqual(left, right);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
