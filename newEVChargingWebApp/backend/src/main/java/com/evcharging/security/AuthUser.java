package com.evcharging.security;

/** The authenticated caller, stored as the Spring Security principal for the current request. */
public record AuthUser(int userId, String email, String role) {
}
