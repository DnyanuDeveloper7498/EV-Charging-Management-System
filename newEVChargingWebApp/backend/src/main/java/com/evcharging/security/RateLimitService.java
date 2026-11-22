package com.evcharging.security;

import com.evcharging.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny in-memory fixed-window rate limiter (per application instance). Good enough for a single server;
 * use Redis / a gateway if you later run several instances.
 */
@Component
public class RateLimitService {

    private record Window(int count, Instant start, Duration length) {
        boolean expired(Instant now) {
            return now.isAfter(start.plus(length));
        }
    }

    private final ConcurrentHashMap<String, Window> hits = new ConcurrentHashMap<>();

    /** Counts one call for the key; returns false once more than {@code max} calls happened inside the window. */
    public boolean tryAcquire(String key, int max, Duration length) {
        Instant now = Instant.now();
        boolean[] allowed = new boolean[1];

        hits.compute(key, (k, w) -> {
            if (w == null || w.expired(now)) {
                allowed[0] = true;
                return new Window(1, now, length);
            }
            if (w.count() >= max) {
                allowed[0] = false;
                return w;
            }
            allowed[0] = true;
            return new Window(w.count() + 1, w.start(), w.length());
        });

        if (hits.size() > 5000) {
            hits.entrySet().removeIf(e -> e.getValue().expired(now));
        }
        return allowed[0];
    }

    /** Same as {@link #tryAcquire} but answers HTTP 429 when the limit is exceeded. */
    public void check(String key, int max, Duration length) {
        if (!tryAcquire(key, max, length)) {
            throw new TooManyAttemptsException("Too many requests. Please try again later.");
        }
    }
}
