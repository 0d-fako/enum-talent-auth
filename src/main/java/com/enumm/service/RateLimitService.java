package com.enumm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, AttemptRecord> attemptCache = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long durationMs;

    public RateLimitService(
            @Value("${rate.limit.max.attempts}") int maxAttempts,
            @Value("${rate.limit.duration}") long durationMs) {
        this.maxAttempts = maxAttempts;
        this.durationMs = durationMs;
    }

    public boolean isRateLimited(String key) {
        cleanupExpiredRecords();

        AttemptRecord record = attemptCache.get(key);
        if (record == null) {
            return false;
        }

        if (record.isExpired(durationMs)) {
            attemptCache.remove(key);
            return false;
        }

        return record.getAttempts() >= maxAttempts;
    }

    public void recordAttempt(String key) {
        cleanupExpiredRecords();

        attemptCache.compute(key, (k, record) -> {
            if (record == null || record.isExpired(durationMs)) {
                return new AttemptRecord(1, LocalDateTime.now());
            }
            return record.increment();
        });
    }

    public void resetAttempts(String key) {
        attemptCache.remove(key);
    }

    private void cleanupExpiredRecords() {
        attemptCache.entrySet().removeIf(entry ->
                entry.getValue().isExpired(durationMs)
        );
    }

    private static class AttemptRecord {
        private final int attempts;
        private final LocalDateTime firstAttempt;

        public AttemptRecord(int attempts, LocalDateTime firstAttempt) {
            this.attempts = attempts;
            this.firstAttempt = firstAttempt;
        }

        public int getAttempts() {
            return attempts;
        }

        public AttemptRecord increment() {
            return new AttemptRecord(attempts + 1, firstAttempt);
        }

        public boolean isExpired(long durationMs) {
            return firstAttempt.plusNanos(durationMs * 1_000_000)
                    .isBefore(LocalDateTime.now());
        }
    }
}