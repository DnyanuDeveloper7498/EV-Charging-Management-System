package com.evcharging.security;

import com.evcharging.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and be at least 32 characters long (generate one with: openssl rand -base64 48).");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMillis = expirationMinutes * 60_000L;
    }

    /** Epoch millis at which a token issued right now will expire. */
    public long expiryFromNow() {
        return System.currentTimeMillis() + expirationMillis;
    }

    public String generateToken(User user, long expiresAtMillis) {
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(expiresAtMillis))
                .signWith(key)
                .compact();
    }

    /** Verifies signature + expiry. Throws io.jsonwebtoken.JwtException when the token is not valid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
