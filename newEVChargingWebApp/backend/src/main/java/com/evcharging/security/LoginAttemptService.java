package com.evcharging.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Very small in-memory brute-force guard: 5 failed logins per e-mail within 15 minutes blocks further attempts. */
@Component
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Entry(int failures, Instant firstFailure) {
    }

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Entry entry = attempts.get(key);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.firstFailure().plus(WINDOW))) {
            attempts.remove(key);
            return false;
        }
        return entry.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.firstFailure().plus(WINDOW))) {
                return new Entry(1, now);
            }
            return new Entry(existing.failures() + 1, existing.firstFailure());
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }
}
