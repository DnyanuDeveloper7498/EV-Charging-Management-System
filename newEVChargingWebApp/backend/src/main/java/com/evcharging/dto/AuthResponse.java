package com.evcharging.dto;

/** Returned by login/register: the JWT plus the (password-free) user. */
public record AuthResponse(String token, String tokenType, long expiresAt, UserResponse user) {
}
