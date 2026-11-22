package com.evcharging.controller;

import com.evcharging.dto.*;
import com.evcharging.model.User;
import com.evcharging.security.RateLimitService;
import com.evcharging.security.SecurityUtils;
import com.evcharging.service.AuthService;
import com.evcharging.service.PasswordResetService;
import com.evcharging.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RateLimitService rateLimit;

    public UserController(UserService userService, AuthService authService, PasswordResetService passwordResetService, RateLimitService rateLimit) {
        this.userService = userService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.rateLimit = rateLimit;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        rateLimit.check("register:" + http.getRemoteAddr(), 10, Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Always answers 200 with the same text, whether or not the e-mail is registered.
     */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimit.check("forgot-ip:" + http.getRemoteAddr(), 10, Duration.ofHours(1));
        passwordResetService.requestReset(request.email());
        return Map.of("message", "If this email is registered, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        rateLimit.check("reset-ip:" + http.getRemoteAddr(), 20, Duration.ofHours(1));
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return Map.of("message", "Password updated. You can now login.");
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(request.fullName(), request.mobileNo()));
    }

    @PutMapping("/me/password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.currentPassword(), request.newPassword());
        return Map.of("message", "Password changed successfully.");
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(userService.findByUserId(SecurityUtils.currentUserId()));
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream().map(UserResponse::from).toList();
    }


    @PostMapping("/department-admins")
    public ResponseEntity<UserResponse> createDepartmentAdmin(@Valid @RequestBody CreateDepartmentAdminRequest request) {

        User admin = userService.createDepartmentAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(admin));
    }

    @PutMapping("/department-admins/{userId}")
    public UserResponse updateDepartmentAdmin(
            @PathVariable int userId,
            @Valid @RequestBody UpdateDepartmentAdminRequest request) {

        return UserResponse.from(
                userService.updateDepartmentAdmin(userId, request)
        );
    }


    @GetMapping("/department-admins")
    public List<UserResponse> getDepartmentAdmins() {

        return userService.getDepartmentAdmins()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @DeleteMapping("/department-admins/{userId}")
    public ResponseEntity<Void> deleteDepartmentAdmin(
            @PathVariable int userId) {

        userService.deleteDepartmentAdmin(userId);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable int userId) {
        return UserResponse.from(userService.findByUserId(userId));
    }

    @PutMapping("/{userId}/role")
    public UserResponse updateRole(@PathVariable int userId, @Valid @RequestBody RoleUpdateRequest request) {
        User updated = userService.updateRole(userId, request.role());
        return UserResponse.from(updated);
    }
}
