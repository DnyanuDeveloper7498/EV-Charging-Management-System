package com.evcharging.service;

import com.evcharging.dto.CreateDepartmentAdminRequest;
import com.evcharging.dto.RegisterRequest;
import com.evcharging.dto.UpdateDepartmentAdminRequest;
import com.evcharging.exception.DuplicateResourceException;
import com.evcharging.exception.UserNotFoundException;
import com.evcharging.model.Roles;
import com.evcharging.model.User;
import com.evcharging.repository.*;
import com.evcharging.security.PasswordUtil;
import com.evcharging.security.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Clock clock;

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationRepository notificationRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StationReviewRepository stationReviewRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(
            UserRepository userRepository,
            Clock clock,
            BookingRepository bookingRepository,
            VehicleRepository vehicleRepository,
            NotificationRepository notificationRepository,
            WalletTransactionRepository walletTransactionRepository,
            StationReviewRepository stationReviewRepository,
            ChargingStationRepository chargingStationRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {

        this.userRepository = userRepository;
        this.clock = clock;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.notificationRepository = notificationRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.stationReviewRepository = stationReviewRepository;
        this.chargingStationRepository = chargingStationRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * Public registration: the role is always USER and the wallet always starts at 0 (never taken from the client).
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setMobileNo(request.mobileNo());
        user.setPassword(PasswordUtil.hash(request.password()));
        user.setWalletBalance(BigDecimal.ZERO);
        user.setRole(Roles.USER);
        user.setCreatedAt(LocalDateTime.now(clock));

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {

            throw new DuplicateResourceException("An account with this email already exists.");
        }
    }


    @Transactional
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);

        if (user == null || !PasswordUtil.matches(rawPassword, user.getPassword())) {
            return null;
        }

        if (!PasswordUtil.isHashed(user.getPassword())) {
            user.setPassword(PasswordUtil.hash(rawPassword));
            user = userRepository.save(user);
        }
        return user;
    }

    @Transactional
    public User updateProfile(String fullName, String mobileNo) {
        User user = userRepository.findById(SecurityUtils.currentUserId()).orElseThrow(() -> new UserNotFoundException("User not found."));
        user.setFullName(fullName.trim());
        user.setMobileNo(mobileNo);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User user = userRepository.findByIdForUpdate(SecurityUtils.currentUserId()).orElseThrow(() -> new UserNotFoundException("User not found."));

        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }
        user.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(user);
    }

    public User findByUserId(int userId) {
        SecurityUtils.requireSelfOrAdmin(userId);
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found."));
    }


    public List<User> getAllUsers() {
        SecurityUtils.requireAdminOrUserAdmin();
        return userRepository.findAllByRole(Roles.USER);
    }


    //----------------------get Department admins -------------

    public List<User> getDepartmentAdmins() {

        SecurityUtils.requireAdmin();

        return userRepository.findAllByRoleIn(Set.of(
                Roles.USER_ADMIN,
                Roles.VEHICLE_ADMIN,
                Roles.CHARGING_ADMIN,
                Roles.FINANCE_ADMIN,
                Roles.SUPPORT_ADMIN,
                Roles.STATION_ADMIN
        ));
    }


    @Transactional
    public User createDepartmentAdmin(CreateDepartmentAdminRequest request) {

        // Only Super Admin can create department admins
        SecurityUtils.requireAdmin();

        String email = request.email().trim().toLowerCase();
        String role = request.role().trim().toUpperCase();

        if (!Set.of(Roles.USER_ADMIN, Roles.VEHICLE_ADMIN, Roles.CHARGING_ADMIN, Roles.FINANCE_ADMIN, Roles.SUPPORT_ADMIN, Roles.STATION_ADMIN).contains(role)) {
            throw new IllegalArgumentException("Invalid department admin role.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User admin = new User();

        admin.setFullName(request.fullName().trim());
        admin.setEmail(email);
        admin.setMobileNo(request.mobileNo().trim());
        admin.setPassword(PasswordUtil.hash(request.password()));
        admin.setWalletBalance(BigDecimal.ZERO);
        admin.setRole(role);
        admin.setCreatedAt(LocalDateTime.now(clock));

        try {
            return userRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Email or mobile number already exists.");
        }
    }


    @Transactional
    public User updateRole(int userId, String role) {

        SecurityUtils.requireAdmin();

        String newRole = role == null ? "" : role.trim().toUpperCase();

        if (!Roles.USER.equals(newRole) && !Roles.STATION_OWNER.equals(newRole)) {
            throw new IllegalArgumentException("Role must be USER or STATION_OWNER.");
        }

        if (userId == SecurityUtils.currentUserId()) {
            throw new IllegalStateException("You cannot change your own role.");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found."));

        // Protect existing Admin account
        if (Roles.ADMIN.equals(user.getRole())) {
            throw new IllegalStateException("Super Admin role cannot be changed.");
        }

        user.setRole(newRole);

        return userRepository.save(user);
    }

//-------------delete department-------------

    @Transactional
    public void deleteDepartmentAdmin(int userId) {

        // Only Super Admin can delete department admins
        SecurityUtils.requireAdmin();

        if (userId == SecurityUtils.currentUserId()) {
            throw new IllegalStateException("You cannot delete your own account.");
        }

        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Department admin not found."));

        Set<String> departmentRoles = Set.of(
                Roles.USER_ADMIN,
                Roles.VEHICLE_ADMIN,
                Roles.CHARGING_ADMIN,
                Roles.FINANCE_ADMIN,
                Roles.SUPPORT_ADMIN,
                Roles.STATION_ADMIN
        );

        if (!departmentRoles.contains(admin.getRole())) {
            throw new IllegalStateException("Only department admin accounts can be deleted.");
        }

        // Prevent deletion if important records are linked
        if (vehicleRepository.countByUserId(userId) > 0
                || bookingRepository.countByUserId(userId) > 0
                || notificationRepository.existsByUserId(userId)
                || walletTransactionRepository.existsByUserId(userId)
                || stationReviewRepository.existsByUserId(userId)
                || chargingStationRepository.existsByOwnerId(userId)) {

            throw new IllegalStateException(
                    "Cannot delete this admin because important records are linked to the account."
            );
        }

        // Remove password reset tokens before deleting the account
        passwordResetTokenRepository.deleteByUserId(userId);

        userRepository.delete(admin);
        userRepository.flush();
    }




    //------------update department ----------------

    @Transactional
    public User updateDepartmentAdmin(int userId, UpdateDepartmentAdminRequest request) {

        // Only Super Admin can update department admins
        SecurityUtils.requireAdmin();

        if (userId == SecurityUtils.currentUserId()) {
            throw new IllegalStateException("You cannot update your own department admin account.");
        }

        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Department admin not found."));

        Set<String> departmentRoles = Set.of(
                Roles.USER_ADMIN,
                Roles.VEHICLE_ADMIN,
                Roles.CHARGING_ADMIN,
                Roles.FINANCE_ADMIN,
                Roles.SUPPORT_ADMIN,
                Roles.STATION_ADMIN
        );

        if (!departmentRoles.contains(admin.getRole())) {
            throw new IllegalStateException("Only department admin accounts can be updated.");
        }

        String newRole = request.role().trim().toUpperCase();

        if (!departmentRoles.contains(newRole)) {
            throw new IllegalArgumentException("Invalid department admin role.");
        }

        admin.setFullName(request.fullName().trim());
        admin.setMobileNo(request.mobileNo().trim());
        admin.setRole(newRole);

        try {
            return userRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Mobile number already exists.");
        }
    }


}
