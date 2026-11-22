package com.evcharging.repository;

import com.evcharging.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserId(int userId);
}
