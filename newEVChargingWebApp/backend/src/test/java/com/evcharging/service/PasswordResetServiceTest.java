package com.evcharging.service;

import com.evcharging.model.PasswordResetToken;
import com.evcharging.model.User;
import com.evcharging.repository.PasswordResetTokenRepository;
import com.evcharging.repository.UserRepository;
import com.evcharging.security.PasswordUtil;
import com.evcharging.security.RateLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneId.of("UTC"));
    private final PasswordResetService service = new PasswordResetService(
            users, tokens, email, new RateLimitService(), clock, "https://app.example.com/", 30);

    private User user() {
        User u = new User();
        u.setUserId(5);
        u.setEmail("a@b.com");
        u.setFullName("Asha Patil");
        u.setPassword(PasswordUtil.hash("OldPassw0rd!"));
        return u;
    }

    @Test
    void requestStoresOnlyAHashAndMailsTheLink() {
        when(users.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(user()));

        service.requestReset("A@B.com ");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(email).send(eq("a@b.com"), anyString(), body.capture());
        Matcher m = Pattern.compile("https://app\\.example\\.com/reset-password\\?token=([A-Za-z0-9_-]+)")
                .matcher(body.getValue());
        assertTrue(m.find(), "mail must contain the reset link");
        String rawToken = m.group(1);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(saved.capture());
        assertEquals(PasswordResetService.sha256(rawToken), saved.getValue().getTokenHash());
        assertNotEquals(rawToken, saved.getValue().getTokenHash());
        assertEquals(LocalDateTime.of(2026, 11, 6, 10, 30), saved.getValue().getExpiresAt());
    }

    @Test
    void unknownEmailSendsNothingAndDoesNotFail() {
        when(users.findByEmailIgnoreCase("nobody@b.com")).thenReturn(Optional.empty());

        service.requestReset("nobody@b.com");

        verifyNoInteractions(email);
        verify(tokens, never()).save(any());
    }

    @Test
    void validTokenChangesThePasswordAndIsConsumed() {
        User u = user();
        PasswordResetToken t = new PasswordResetToken();
        t.setUserId(5);
        t.setTokenHash(PasswordResetService.sha256("tok"));
        t.setExpiresAt(LocalDateTime.now(clock).plusMinutes(5));
        when(tokens.findByTokenHash(PasswordResetService.sha256("tok"))).thenReturn(Optional.of(t));
        when(users.findByIdForUpdate(5)).thenReturn(Optional.of(u));

        service.resetPassword("tok", "NewPassw0rd!");

        assertTrue(PasswordUtil.matches("NewPassw0rd!", u.getPassword()));
        assertTrue(t.isUsed());
    }

    @Test
    void expiredTokenIsRejected() {
        PasswordResetToken t = new PasswordResetToken();
        t.setUserId(5);
        t.setExpiresAt(LocalDateTime.now(clock).minusMinutes(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(t));

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("tok", "NewPassw0rd!"));
        verify(users, never()).save(any());
    }

    @Test
    void usedOrUnknownTokenIsRejected() {
        PasswordResetToken used = new PasswordResetToken();
        used.setUserId(5);
        used.setUsed(true);
        used.setExpiresAt(LocalDateTime.now(clock).plusMinutes(5));
        when(tokens.findByTokenHash(PasswordResetService.sha256("used"))).thenReturn(Optional.of(used));
        when(tokens.findByTokenHash(PasswordResetService.sha256("unknown"))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("used", "NewPassw0rd!"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("unknown", "NewPassw0rd!"));
    }
}
