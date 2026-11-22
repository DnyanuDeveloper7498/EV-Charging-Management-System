package com.evcharging.repository;

import com.evcharging.model.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findBySessionId(int sessionId);

    List<Payment> findByPaymentStatus(String paymentStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.sessionId = :sessionId")
    Optional<Payment> findBySessionIdForUpdate(@Param("sessionId") int sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.razorpayOrderId = :orderId")
    Optional<Payment> findByRazorpayOrderIdForUpdate(@Param("orderId") String orderId);

    @Query("select p from Payment p, ChargingSession s, Booking b, Vehicle v "
            + "where p.sessionId = s.sessionId and s.bookingId = b.bookingId and b.vehicleId = v.vehicleId "
            + "and v.userId = :userId order by p.paymentTime desc")
    List<Payment> findByUserId(@Param("userId") int userId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.paymentStatus = 'SUCCESS'")
    BigDecimal sumSuccessfulAmount();

    @Query("select coalesce(sum(p.amount), 0) from Payment p, ChargingSession s, Booking b, Vehicle v "
            + "where p.sessionId = s.sessionId and s.bookingId = b.bookingId and b.vehicleId = v.vehicleId "
            + "and v.userId = :userId and p.paymentStatus = 'SUCCESS'")
    BigDecimal sumSuccessfulAmountByUser(@Param("userId") int userId);
}
