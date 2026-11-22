package com.evcharging.service;

import com.evcharging.model.Roles;
import com.evcharging.model.User;
import com.evcharging.repository.UserRepository;
import com.evcharging.security.PasswordUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;

    private final String adminName;
    private final String adminEmail;
    private final String adminMobile;
    private final String adminPassword;

    public AdminBootstrap(UserRepository userRepository, @Value("${app.super-admin.name:}") String adminName, @Value("${app.super-admin.email:}") String adminEmail, @Value("${app.super-admin.mobile:}") String adminMobile, @Value("${app.super-admin.password:}") String adminPassword) {

        this.userRepository = userRepository;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminMobile = adminMobile;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (adminEmail.isBlank() || adminName.isBlank() || adminMobile.isBlank() || adminPassword.isBlank()) {

            log.warn("Super Admin configuration is missing.");
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        User existingUser = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (existingUser != null) {

            if (!Roles.ADMIN.equals(existingUser.getRole())) {
                throw new IllegalStateException("Configured Super Admin email already belongs to a non-admin account. " + "Use a dedicated admin email.");
            }

            log.info("Super Admin account already exists.");
            return;
        }

        User admin = new User();

        admin.setFullName(adminName.trim());
        admin.setEmail(email);
        admin.setMobileNo(adminMobile.trim());
        admin.setPassword(PasswordUtil.hash(adminPassword));
        admin.setWalletBalance(BigDecimal.ZERO);
        admin.setRole(Roles.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());

        userRepository.save(admin);

        log.info("Super Admin account created successfully.");
    }
}
