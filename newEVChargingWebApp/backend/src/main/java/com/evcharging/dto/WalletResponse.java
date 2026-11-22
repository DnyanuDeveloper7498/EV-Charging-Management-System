package com.evcharging.dto;

import com.evcharging.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Wallet view. It deliberately carries the same user fields the frontend keeps in its session
 * (userId, fullName, walletBalance ...) so the UI can refresh its stored user from it.
 */
public record WalletResponse(
        int userId,
        String fullName,
        String email,
        String mobileNo,
        BigDecimal walletBalance,
        String role,
        LocalDateTime createdAt,
        boolean demoTopupEnabled) {

    public static WalletResponse of(User user, boolean demoTopupEnabled) {
        return new WalletResponse(user.getUserId(), user.getFullName(), user.getEmail(), user.getMobileNo(),
                user.getWalletBalance(), user.getRole(), user.getCreatedAt(), demoTopupEnabled);
    }
}
