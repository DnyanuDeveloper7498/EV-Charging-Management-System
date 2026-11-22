package com.evcharging.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private int tokenId;

    @Column(name = "user_id")
    private int userId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used")
    private boolean used;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    public int getTokenId() { return tokenId; }

    public int getUserId() { return userId; }

    public void setUserId(int userId) { this.userId = userId; }

    public String getTokenHash() { return tokenHash; }

    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }

    public void setUsed(boolean used) { this.used = used; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
