package com.enumm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private static final int MAX_ATTEMPTS = 5;
    private static final long DURATION_MS = 1000L;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(MAX_ATTEMPTS, DURATION_MS);
    }

    @Test
    void isRateLimited_shouldReturnFalseForNewKey() {
        assertFalse(rateLimitService.isRateLimited("test@example.com"));
    }

    @Test
    void isRateLimited_shouldReturnFalseWhenBelowLimit() {
        String email = "test@example.com";

        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            rateLimitService.recordAttempt(email);
        }

        assertFalse(rateLimitService.isRateLimited(email));
    }

    @Test
    void isRateLimited_shouldReturnTrueWhenLimitReached() {
        String email = "test@example.com";

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitService.recordAttempt(email);
        }

        assertTrue(rateLimitService.isRateLimited(email));
    }

    @Test
    void isRateLimited_shouldReturnTrueWhenLimitExceeded() {
        String email = "test@example.com";

        for (int i = 0; i < MAX_ATTEMPTS + 2; i++) {
            rateLimitService.recordAttempt(email);
        }

        assertTrue(rateLimitService.isRateLimited(email));
    }

    @Test
    void resetAttempts_shouldClearRateLimit() {
        String email = "test@example.com";

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitService.recordAttempt(email);
        }

        assertTrue(rateLimitService.isRateLimited(email));

        rateLimitService.resetAttempts(email);
        assertFalse(rateLimitService.isRateLimited(email));
    }

    @Test
    void isRateLimited_shouldExpireAfterDuration() throws InterruptedException {
        String email = "test@example.com";

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitService.recordAttempt(email);
        }

        assertTrue(rateLimitService.isRateLimited(email));

        Thread.sleep(DURATION_MS + 100);

        assertFalse(rateLimitService.isRateLimited(email));
    }

    @Test
    void recordAttempt_shouldHandleMultipleKeys() {
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            rateLimitService.recordAttempt(email1);
        }

        assertTrue(rateLimitService.isRateLimited(email1));
        assertFalse(rateLimitService.isRateLimited(email2));
    }
}