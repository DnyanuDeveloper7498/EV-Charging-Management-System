package com.evcharging.repository;

import com.evcharging.model.ChargingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Integer> {

    Optional<ChargingSession> findByBookingId(int bookingId);

    List<ChargingSession> findByEndTimeIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChargingSession s where s.sessionId = :sessionId")
    Optional<ChargingSession> findByIdForUpdate(@Param("sessionId") int sessionId);

    @Query("select s from ChargingSession s, Booking b, Vehicle v "
            + "where s.bookingId = b.bookingId and b.vehicleId = v.vehicleId and v.userId = :userId "
            + "order by s.startTime desc")
    List<ChargingSession> findByUserId(@Param("userId") int userId);

    @Query("select coalesce(sum(s.unitsConsumed), 0) from ChargingSession s")
    BigDecimal sumUnitsConsumed();

    @Query("select coalesce(sum(s.unitsConsumed), 0) from ChargingSession s, Booking b, Vehicle v "
            + "where s.bookingId = b.bookingId and b.vehicleId = v.vehicleId and v.userId = :userId")
    BigDecimal sumUnitsConsumedByUser(@Param("userId") int userId);

    @Query("select count(s) from ChargingSession s, Booking b, Vehicle v "
            + "where s.bookingId = b.bookingId and b.vehicleId = v.vehicleId and v.userId = :userId")
    long countByUser(@Param("userId") int userId);
}
