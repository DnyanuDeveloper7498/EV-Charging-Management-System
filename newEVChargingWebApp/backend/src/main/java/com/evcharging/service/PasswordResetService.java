package com.evcharging.service;

import com.evcharging.model.PasswordResetToken;
import com.evcharging.model.User;
import com.evcharging.repository.PasswordResetTokenRepository;
import com.evcharging.repository.UserRepository;
import com.evcharging.security.PasswordUtil;
import com.evcharging.security.RateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Forgot / reset password. Only a SHA-256 hash of the token is stored; the raw token is only inside the e-mailed
 * link. The request endpoint never reveals whether an e-mail address is registered.
 */
@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimit;
    private final Clock clock;
    private final String frontendUrl;
    private final long expiryMinutes;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                RateLimitService rateLimit,
                                Clock clock,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                                @Value("${app.password-reset.expiry-minutes:30}") long expiryMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.rateLimit = rateLimit;
        this.clock = clock;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.expiryMinutes = expiryMinutes;
    }

    @Transactional
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();

        // at most 3 mails per address per hour; over the limit we silently do nothing (no information leak)
        if (!rateLimit.tryAcquire("forgot-email:" + email, 3, Duration.ofHours(1))) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return;
        }

        tokenRepository.deleteByUserId(user.getUserId());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUserId(user.getUserId());
        entity.setTokenHash(sha256(token));
        entity.setCreatedAt(LocalDateTime.now(clock));
        entity.setExpiresAt(LocalDateTime.now(clock).plusMinutes(expiryMinutes));
        tokenRepository.save(entity);

        String link = frontendUrl + "/reset-password?token=" + token;
        String body = "Hi " + user.getFullName() + ",\n\n"
                + "We received a request to reset your Smart EV password.\n"
                + "Open this link to choose a new password (valid for " + expiryMinutes + " minutes):\n\n"
                + link + "\n\n"
                + "If you did not ask for this, you can safely ignore this e-mail.\n\n"
                + "Smart EV Charging";
        emailService.send(user.getEmail(), "Reset your Smart EV password", body);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken entity = tokenRepository.findByTokenHash(sha256(token.trim())).orElse(null);

        if (entity == null || entity.isUsed() || !entity.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("This reset link is invalid or has expired. Please request a new one.");
        }

        User user = userRepository.findByIdForUpdate(entity.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired."));

        user.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(user);

        entity.setUsed(true);
        tokenRepository.save(entity);
        tokenRepository.deleteByUserId(user.getUserId());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available.", e);
        }
    }
}
