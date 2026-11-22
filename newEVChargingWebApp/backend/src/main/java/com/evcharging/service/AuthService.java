package com.evcharging.service;

import com.evcharging.dto.AuthResponse;
import com.evcharging.dto.LoginRequest;
import com.evcharging.dto.RegisterRequest;
import com.evcharging.dto.UserResponse;
import com.evcharging.exception.TooManyAttemptsException;
import com.evcharging.model.User;
import com.evcharging.security.JwtService;
import com.evcharging.security.LoginAttemptService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    public AuthService(UserService userService, JwtService jwtService, LoginAttemptService loginAttempts) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
    }

    public AuthResponse register(RegisterRequest request) {
        return toResponse(userService.registerUser(request));
    }

    /** Returns null when the credentials are wrong (the controller answers 401). */
    public AuthResponse login(LoginRequest request) {
        String key = request.email().trim().toLowerCase();

        if (loginAttempts.isBlocked(key)) {
            throw new TooManyAttemptsException("Too many failed login attempts. Please try again in 15 minutes.");
        }

        User user = userService.authenticate(request.email(), request.password());

        if (user == null) {
            loginAttempts.recordFailure(key);
            return null;
        }

        loginAttempts.recordSuccess(key);
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        long expiresAt = jwtService.expiryFromNow();
        return new AuthResponse(jwtService.generateToken(user, expiresAt), "Bearer", expiresAt,
                UserResponse.from(user));
    }
}
