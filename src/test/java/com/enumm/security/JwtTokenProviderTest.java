package com.enumm.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String secret;
    private long expirationMs;

    @BeforeEach
    void setUp() {
        secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        expirationMs = 604800000L;
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtTokenProvider.generateToken("test@example.com");
        assertNotNull(token);
    }

    @Test
    void generateToken_shouldReturnValidJwtFormat() {
        String token = jwtTokenProvider.generateToken("test@example.com");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
    }

    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String email = "test@example.com";
        String token = jwtTokenProvider.generateToken(email);

        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateToken("test@example.com");
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.jwt.token";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date past = new Date(System.currentTimeMillis() - 1000);

        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(past)
                .expiration(past)
                .signWith(key)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_shouldReturnFalseForTamperedToken() {
        String token = jwtTokenProvider.generateToken("test@example.com");
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }
}