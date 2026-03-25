package com.example.auth.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmacServiceTest {

    private final HmacService hmacService = new HmacService();

    @Test
    void hmacSha256Hex_shouldBeDeterministic() {
        String a = hmacService.hmacSha256Hex("secret", "email:nonce:123");
        String b = hmacService.hmacSha256Hex("secret", "email:nonce:123");
        assertEquals(a, b);
    }

    @Test
    void hmacSha256Hex_shouldChangeWhenMessageChanges() {
        String a = hmacService.hmacSha256Hex("secret", "m1");
        String b = hmacService.hmacSha256Hex("secret", "m2");
        assertNotEquals(a, b);
    }

    @Test
    void constantTimeEqualsHex_shouldReturnTrueForSameHex() {
        String hex = hmacService.hmacSha256Hex("secret", "payload");
        assertTrue(hmacService.constantTimeEqualsHex(hex, hex));
    }

    @Test
    void constantTimeEqualsHex_shouldReturnFalseForDifferentHex() {
        assertFalse(hmacService.constantTimeEqualsHex("aa", "bb"));
    }

    @Test
    void constantTimeEqualsHex_shouldReturnFalseOnNullOrInvalidHex() {
        assertFalse(hmacService.constantTimeEqualsHex(null, "aa"));
        assertFalse(hmacService.constantTimeEqualsHex("zz", "aa"));
    }
}
