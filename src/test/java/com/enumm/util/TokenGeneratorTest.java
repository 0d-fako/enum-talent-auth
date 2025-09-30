package com.enumm.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TokenGeneratorTest {

    @InjectMocks
    private TokenGenerator tokenGenerator;

    @Test
    void generateVerificationToken_shouldReturnNonNullToken() {
        String token = tokenGenerator.generateVerificationToken();
        assertNotNull(token);
    }

    @Test
    void generateVerificationToken_shouldReturnNonEmptyToken() {
        String token = tokenGenerator.generateVerificationToken();
        assertFalse(token.isEmpty());
    }

    @Test
    void generateVerificationToken_shouldGenerateUniqueTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String token = tokenGenerator.generateVerificationToken();
            tokens.add(token);
        }
        assertEquals(100, tokens.size(), "All generated tokens should be unique");
    }

    @Test
    void generateVerificationToken_shouldGenerateUrlSafeToken() {
        String token = tokenGenerator.generateVerificationToken();
        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));
    }
}