package com.evcharging.repository;

import com.evcharging.model.WalletTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(int userId);

    boolean existsByUserId(int userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTransaction t where t.razorpayOrderId = :orderId")
    Optional<WalletTransaction> findByRazorpayOrderIdForUpdate(@Param("orderId") String orderId);
}
