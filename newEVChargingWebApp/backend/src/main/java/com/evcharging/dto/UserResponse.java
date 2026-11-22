package com.evcharging.dto;

import com.evcharging.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Safe view of a user: never contains the password hash. */
public record UserResponse(
        int userId,
        String fullName,
        String email,
        String mobileNo,
        BigDecimal walletBalance,
        String role,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getUserId(), user.getFullName(), user.getEmail(), user.getMobileNo(),
                user.getWalletBalance(), user.getRole(), user.getCreatedAt());
    }
}
