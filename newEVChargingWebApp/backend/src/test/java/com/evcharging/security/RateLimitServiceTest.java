package com.evcharging.security;

import com.evcharging.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private final RateLimitService limiter = new RateLimitService();

    @Test
    void blocksAfterTheLimitIsReached() {
        Duration hour = Duration.ofHours(1);

        assertTrue(limiter.tryAcquire("k", 3, hour));
        assertTrue(limiter.tryAcquire("k", 3, hour));
        assertTrue(limiter.tryAcquire("k", 3, hour));
        assertFalse(limiter.tryAcquire("k", 3, hour));
    }

    @Test
    void keysAreIndependent() {
        Duration hour = Duration.ofHours(1);

        assertTrue(limiter.tryAcquire("a", 1, hour));
        assertFalse(limiter.tryAcquire("a", 1, hour));
        assertTrue(limiter.tryAcquire("b", 1, hour));
    }

    @Test
    void checkThrowsTooManyAttempts() {
        limiter.check("x", 1, Duration.ofMinutes(5));
        assertThrows(TooManyAttemptsException.class, () -> limiter.check("x", 1, Duration.ofMinutes(5)));
    }
}
