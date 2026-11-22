package com.evcharging.repository;

import com.evcharging.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByVehicleId(int vehicleId);

    List<Booking> findByStationId(int stationId);

    boolean existsByStationId(int stationId);

    List<Booking> findByBookingStatus(String bookingStatus);

    long countByVehicleIdAndBookingStatus(int vehicleId, String bookingStatus);

    @Query("select b from Booking b where b.vehicleId in " + "(select v.vehicleId from Vehicle v where v.userId = :userId) order by b.bookingTime desc")
    List<Booking> findByUserId(@Param("userId") int userId);


    @Query("""
            SELECT b FROM Booking b
            WHERE b.stationId IN (
                SELECT s.stationId
                FROM ChargingStation s
                WHERE s.ownerId = :ownerId
            )
            AND b.bookingStatus = :status
            ORDER BY b.bookingTime ASC
            """)
    List<Booking> findBookingsByStationOwner(@Param("ownerId") int ownerId, @Param("status") String status);

    @Query("select count(b) from Booking b where b.vehicleId in " + "(select v.vehicleId from Vehicle v where v.userId = :userId)")
    long countByUserId(@Param("userId") int userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.bookingId = :bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") int bookingId);

    /**
     * Active bookings of one vehicle whose start time lies strictly inside (from, to).
     */
    @Query("select count(b) from Booking b where b.vehicleId = :vehicleId " + "and b.bookingStatus in :statuses " + "and b.bookingTime > :from and b.bookingTime < :to")
    long countOverlapping(@Param("vehicleId") int vehicleId, @Param("statuses") Collection<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    @Query("select count(b) from Booking b where b.vehicleId = :vehicleId " + "and b.bookingId <> :bookingId " + "and b.bookingStatus in :statuses " + "and b.bookingTime > :from and b.bookingTime < :to")
    long countOverlappingExcludingBooking(@Param("vehicleId") int vehicleId, @Param("bookingId") int bookingId, @Param("statuses") Collection<String> statuses, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    @Query("select b.bookingId from Booking b where b.bookingStatus = :status and b.bookingTime < :before")
    List<Integer> findIdsByStatusAndTimeBefore(@Param("status") String status, @Param("before") LocalDateTime before);


    @Query("select b.bookingId from Booking b " + "where b.bookingStatus = :status " + "and b.bookingTime > :from " + "and b.bookingTime <= :to " + "and b.reminderSentAt is null")
    List<Integer> findBookingsForReminder(@Param("status") String status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    @Query("""
            SELECT b.bookingId
            FROM Booking b
            WHERE b.bookingStatus = :status
            AND b.approvalExpiresAt <= :now
            """)
    List<Integer> findExpiredApprovalBookingIds(@Param("status") String status, @Param("now") LocalDateTime now);
}
