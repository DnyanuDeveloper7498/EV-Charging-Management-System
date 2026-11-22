package com.evcharging.service;

import com.evcharging.dto.CancelBookingRequest;
import com.evcharging.dto.CreateBookingRequest;
import com.evcharging.dto.RescheduleBookingRequest;
import com.evcharging.exception.BookingNotFoundException;
import com.evcharging.exception.ChargingStationNotFoundException;
import com.evcharging.exception.VehicleNotFoundException;
import com.evcharging.model.*;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.VehicleRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking lifecycle: CONFIRMED -> IN_PROGRESS -> COMPLETED, or CONFIRMED -> CANCELLED / EXPIRED.
 * <p>
 * Lock order used everywhere to avoid deadlocks: session -> booking -> vehicle -> station.
 * Slot allocation always happens while holding the station row lock, so the same slot can never be
 * handed to two users.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final AccessService access;
    private final Clock clock;

    private final long slotMinutes;
    private final long graceMinutes;
    private final long maxAdvanceDays;

    private final InAppNotificationService inAppNotificationService;

    public BookingService(BookingRepository bookingRepository, VehicleRepository vehicleRepository, ChargingStationRepository chargingStationRepository, AccessService access, Clock clock, InAppNotificationService inAppNotificationService, @Value("${app.booking.slot-minutes}") long slotMinutes, @Value("${app.booking.grace-minutes}") long graceMinutes, @Value("${app.booking.max-advance-days}") long maxAdvanceDays) {
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.chargingStationRepository = chargingStationRepository;
        this.access = access;
        this.clock = clock;
        this.inAppNotificationService = inAppNotificationService;
        this.slotMinutes = slotMinutes;
        this.graceMinutes = graceMinutes;
        this.maxAdvanceDays = maxAdvanceDays;


    }

    // ------------------------------------------------------------------ reads

    public Booking findByBookingId(int bookingId) {
        return access.bookingForCurrentUserOrAdmin(bookingId);
    }

    public List<Booking> getAllBookings() {
        SecurityUtils.requireAdminOrChargingAdmin();
        return bookingRepository.findAll();
    }

    public List<Booking> findByVehicleId(int vehicleId) {
        access.vehicleForCurrentUserOrAdmin(vehicleId);
        return bookingRepository.findByVehicleId(vehicleId);
    }

    public List<Booking> findByStationId(int stationId) {
        SecurityUtils.requireAdminOrChargingAdmin();
        return bookingRepository.findByStationId(stationId);
    }

    public List<Booking> findByUserId(int userId) {
        SecurityUtils.requireSelfOrAdmin(userId);
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> findByBookingStatus(String bookingStatus) {
        SecurityUtils.requireAdminOrChargingAdmin();
        return bookingRepository.findByBookingStatus(bookingStatus.toUpperCase());
    }


    public List<Booking> getPendingBookingsForOwner() {

        var currentUser = SecurityUtils.currentUser();

        if (!Roles.STATION_OWNER.equals(currentUser.role())) {
            throw new AccessDeniedException("Station owner access required.");
        }

        return bookingRepository.findBookingsByStationOwner(currentUser.userId(), BookingStatus.PENDING_APPROVAL);
    }

    // ----------------------------------------------------------------- create

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        int userId = SecurityUtils.currentUserId();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime time = request.bookingTime();

        if (!time.isAfter(now)) {
            throw new IllegalArgumentException("Booking time must be in the future.");
        }
        if (time.isAfter(now.plusDays(maxAdvanceDays))) {
            throw new IllegalArgumentException("Bookings can be made at most " + maxAdvanceDays + " days in advance.");
        }

        // 1) lock the vehicle: serialises concurrent bookings for the same vehicle
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.vehicleId()).orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));

        // vehicle must belong to the caller (admins cannot book on behalf of someone else)
        if (vehicle.getUserId() != userId) {
            throw new VehicleNotFoundException("Vehicle not found.");
        }

        // 2) lock the station: serialises slot allocation
        ChargingStation station = chargingStationRepository.findByIdForUpdate(request.stationId()).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));

        if (!station.isBookable()) {
            throw new IllegalArgumentException("Charging station is not available.");
        }
        if (station.getavailableSlots() <= 0) {
            throw new IllegalArgumentException("No slots available.");
        }

        if (bookingRepository.countByVehicleIdAndBookingStatus(vehicle.getVehicleId(), BookingStatus.IN_PROGRESS) > 0) {
            throw new IllegalStateException("This vehicle is currently charging.");
        }

        long overlapping = bookingRepository.countOverlapping(vehicle.getVehicleId(), BookingStatus.ACTIVE, time.minusMinutes(slotMinutes), time.plusMinutes(slotMinutes));
        if (overlapping > 0) {
            throw new IllegalStateException("This vehicle already has a booking within " + slotMinutes + " minutes of the selected time.");
        }


        boolean manualApproval = "MANUAL".equalsIgnoreCase(station.getBookingMode());

        if (manualApproval && station.getOwnerId() == null) {
            throw new IllegalStateException("Station owner is not assigned.");
        }

        station.setavailableSlots(station.getavailableSlots() - 1);
        chargingStationRepository.save(station);

        String status = manualApproval ? BookingStatus.PENDING_APPROVAL : BookingStatus.CONFIRMED;

        Booking booking = new Booking(status, time, station.getstationId(), vehicle.getVehicleId());

        if (manualApproval) {
            booking.setApprovalExpiresAt(now.plusMinutes(5));
        }

        Booking savedBooking = bookingRepository.save(booking);

        if (manualApproval) {

            inAppNotificationService.createNotification(userId, savedBooking.getbookingId(), "Booking Pending Approval", "Your booking #" + savedBooking.getbookingId() + " is waiting for station owner approval.", "BOOKING_PENDING_APPROVAL");

            inAppNotificationService.createNotification(station.getOwnerId(), savedBooking.getbookingId(), "New Booking Request", "A new booking request #" + savedBooking.getbookingId() + " requires your approval.", "BOOKING_APPROVAL_REQUEST");

        } else {

            inAppNotificationService.createNotification(userId, savedBooking.getbookingId(), "Booking Confirmed", "Your charging slot has been successfully booked. Booking ID: #" + savedBooking.getbookingId() + ". Booking time: " + time, "BOOKING_CONFIRMATION");
        }

        return savedBooking;

    }

//-----------------------------------approveBooking--------------------------

    @Transactional
    public Booking approveBooking(int bookingId) {

        var currentUser = SecurityUtils.currentUser();

        if (!com.evcharging.model.Roles.STATION_OWNER.equals(currentUser.role())) {
            throw new org.springframework.security.access.AccessDeniedException("Station owner access required.");
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        ChargingStation station = chargingStationRepository.findByIdForUpdate(booking.getstationId()).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));

        if (currentUser.userId() != station.getOwnerId()) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot manage bookings for this station.");
        }

        if (!BookingStatus.PENDING_APPROVAL.equals(booking.getbookingStatus())) {
            throw new IllegalStateException("Booking is not waiting for approval.");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (booking.getApprovalExpiresAt() == null || !booking.getApprovalExpiresAt().isAfter(now)) {

            releaseSlot(booking.getstationId());

            booking.setbookingStatus(BookingStatus.EXPIRED);
            booking.setApprovalExpiresAt(null);

            Booking expiredBooking = bookingRepository.save(booking);

            int customerId = access.ownerOfBooking(expiredBooking);

            inAppNotificationService.createNotification(customerId, expiredBooking.getbookingId(), "Booking Expired", "Your booking #" + expiredBooking.getbookingId() + " expired because the station owner did not approve it in time.", "BOOKING_EXPIRED");

            return expiredBooking;
        }

        booking.setbookingStatus(BookingStatus.CONFIRMED);
        booking.setApprovalExpiresAt(null);

        Booking savedBooking = bookingRepository.save(booking);

        int customerId = access.ownerOfBooking(savedBooking);

        inAppNotificationService.createNotification(customerId, savedBooking.getbookingId(), "Booking Confirmed", "Your booking #" + savedBooking.getbookingId() + " has been approved by the station owner.", "BOOKING_CONFIRMATION");

        return savedBooking;
    }


    //-----------------------------------rejectBooking-------------------

    @Transactional
    public Booking rejectBooking(int bookingId) {

        var currentUser = SecurityUtils.currentUser();

        if (!com.evcharging.model.Roles.STATION_OWNER.equals(currentUser.role())) {
            throw new org.springframework.security.access.AccessDeniedException("Station owner access required.");
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        ChargingStation station = chargingStationRepository.findByIdForUpdate(booking.getstationId()).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));

        if (currentUser.userId() != station.getOwnerId()) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot manage bookings for this station.");
        }

        if (!BookingStatus.PENDING_APPROVAL.equals(booking.getbookingStatus())) {
            throw new IllegalStateException("Booking is not waiting for approval.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int customerId = access.ownerOfBooking(booking);

        if (booking.getApprovalExpiresAt() == null || !booking.getApprovalExpiresAt().isAfter(now)) {

            releaseSlot(booking.getstationId());

            booking.setbookingStatus(BookingStatus.EXPIRED);
            booking.setApprovalExpiresAt(null);

            Booking expiredBooking = bookingRepository.save(booking);

            inAppNotificationService.createNotification(customerId, expiredBooking.getbookingId(), "Booking Expired", "Your booking #" + expiredBooking.getbookingId() + " expired because the owner did not respond in time.", "BOOKING_EXPIRED");

            return expiredBooking;
        }

        releaseSlot(booking.getstationId());

        booking.setbookingStatus(BookingStatus.REJECTED);
        booking.setApprovalExpiresAt(null);

        Booking savedBooking = bookingRepository.save(booking);

        inAppNotificationService.createNotification(customerId, savedBooking.getbookingId(), "Booking Rejected", "Your booking #" + savedBooking.getbookingId() + " was rejected by the station owner.", "BOOKING_REJECTED");

        return savedBooking;
    }


    // ------------------------------------------------------------ reschedule

    @Transactional
    public Booking rescheduleBooking(int bookingId, RescheduleBookingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime newTime = request.getBookingTime();

        if (newTime == null) {
            throw new IllegalArgumentException("New booking time is required.");
        }

        if (!newTime.isAfter(now)) {
            throw new IllegalArgumentException("Booking time must be in the future.");
        }

        if (newTime.isAfter(now.plusDays(maxAdvanceDays))) {
            throw new IllegalArgumentException("Bookings can be made at most " + maxAdvanceDays + " days in advance.");
        }

        // Lock existing booking
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        // Verify booking ownership
        if (!SecurityUtils.isSelfOrAdmin(access.ownerOfBooking(booking))) {
            throw new BookingNotFoundException("Booking not found.");
        }

        // Only confirmed bookings can be rescheduled
        if (!BookingStatus.CONFIRMED.equalsIgnoreCase(booking.getbookingStatus())) {
            throw new IllegalStateException("Only confirmed bookings can be rescheduled.");
        }

        // Lock vehicle to prevent simultaneous bookings
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(booking.getvehicleId()).orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));

        // Check whether vehicle is already charging
        if (bookingRepository.countByVehicleIdAndBookingStatus(vehicle.getVehicleId(), BookingStatus.IN_PROGRESS) > 0) {
            throw new IllegalStateException("This vehicle is currently charging.");
        }

        // Check conflicting bookings, excluding current booking
        long overlapping = bookingRepository.countOverlappingExcludingBooking(vehicle.getVehicleId(), booking.getbookingId(), BookingStatus.ACTIVE, newTime.minusMinutes(slotMinutes), newTime.plusMinutes(slotMinutes));

        if (overlapping > 0) {
            throw new IllegalStateException("This vehicle already has a booking within " + slotMinutes + " minutes of the selected time.");
        }

        // Update booking time
        LocalDateTime oldTime = booking.getbookingTime();

        booking.setbookingTime(newTime);
        booking.setReminderSentAt(null);

        int ownerId = access.ownerOfBooking(booking);

        Booking savedBooking = bookingRepository.save(booking);

        inAppNotificationService.createNotification(ownerId, savedBooking.getbookingId(), "Booking Rescheduled", "Your booking #" + savedBooking.getbookingId() + " has been rescheduled from " + oldTime + " to " + newTime, "BOOKING_RESCHEDULED");

        return savedBooking;
    }


    // ----------------------------------------------------------------- cancel

    @Transactional
    public void cancelBooking(int bookingId, CancelBookingRequest request) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        if (!SecurityUtils.isSelfOrAdmin(access.ownerOfBooking(booking))) {
            throw new BookingNotFoundException("Booking not found.");
        }

        if (!BookingStatus.CONFIRMED.equalsIgnoreCase(booking.getbookingStatus())) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled.");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (!booking.getbookingTime().isAfter(now)) {
            throw new IllegalStateException("Past bookings cannot be cancelled.");
        }

        // Cancellation policy: 2 hours
        boolean freeCancellation = booking.getbookingTime().isAfter(now.plusHours(2));

        String cancellationType = freeCancellation ? "FREE_CANCELLATION" : "LATE_CANCELLATION";

        String reason = request == null ? null : request.getCancellationReason();

        // Release station slot
        releaseSlot(booking.getstationId());

        // Update booking details
        booking.setbookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancellationType(cancellationType);
        booking.setCancellationReason(reason);
        booking.setRefundStatus("NOT_APPLICABLE");

        int ownerId = access.ownerOfBooking(booking);

        Booking savedBooking = bookingRepository.save(booking);


        inAppNotificationService.createNotification(ownerId, savedBooking.getbookingId(), "Booking Cancelled", "Your booking #" + savedBooking.getbookingId() + " has been cancelled. Reason: " + (reason == null || reason.isBlank() ? "Not provided" : reason), "BOOKING_CANCELLATION");
    }

//-----------------------------------------------------------------------------------------------------------reminder

    @Transactional(readOnly = true)
    public List<Integer> findBookingReminderIds() {
        LocalDateTime now = LocalDateTime.now(clock);

        return bookingRepository.findBookingsForReminder(BookingStatus.CONFIRMED, now.plusMinutes(10), now.plusMinutes(15));
    }

    @Transactional
    public void sendBookingReminder(int bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);

        if (booking == null) {
            return;
        }

        if (!BookingStatus.CONFIRMED.equals(booking.getbookingStatus())) {
            return;
        }

        if (booking.getReminderSentAt() != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (booking.getbookingTime().isBefore(now.plusMinutes(10)) || booking.getbookingTime().isAfter(now.plusMinutes(15))) {
            return;
        }

        int userId = access.ownerOfBooking(booking);

        booking.setReminderSentAt(now);
        bookingRepository.save(booking);

        inAppNotificationService.createNotification(userId, booking.getbookingId(), "Charging Reminder", "Your charging booking #" + booking.getbookingId() + " starts in approximately 10-15 minutes.", "BOOKING_REMINDER");
    }


    // ----------------------------------------------------------------- expiry


    @Transactional(readOnly = true)
    public List<Integer> findExpiredApprovalBookingIds() {
        LocalDateTime now = LocalDateTime.now(clock);

        return bookingRepository.findExpiredApprovalBookingIds(BookingStatus.PENDING_APPROVAL, now);
    }

    @Transactional
    public void expirePendingApprovalBooking(int bookingId) {

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);

        if (booking == null || !BookingStatus.PENDING_APPROVAL.equals(booking.getbookingStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (booking.getApprovalExpiresAt() == null || booking.getApprovalExpiresAt().isAfter(now)) {
            return;
        }

        releaseSlot(booking.getstationId());

        booking.setbookingStatus(BookingStatus.EXPIRED);
        booking.setApprovalExpiresAt(null);

        int userId = access.ownerOfBooking(booking);

        Booking savedBooking = bookingRepository.save(booking);

        inAppNotificationService.createNotification(userId, savedBooking.getbookingId(), "Booking Request Expired", "Your booking #" + savedBooking.getbookingId() + " expired because the station owner did not approve it within 5 minutes.", "BOOKING_EXPIRED");
    }


    /**
     * Ids of CONFIRMED bookings whose start time passed more than the grace period ago.
     */
    @Transactional(readOnly = true)
    public List<Integer> findExpiredBookingIds() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(graceMinutes);
        return bookingRepository.findIdsByStatusAndTimeBefore(BookingStatus.CONFIRMED, cutoff);
    }

    /**
     * Expires one overdue booking and frees its slot. Safe to call repeatedly / from several instances.
     */
    @Transactional
    public void expireBooking(int bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);

        if (booking == null || !BookingStatus.CONFIRMED.equals(booking.getbookingStatus())) {
            return; // already started, cancelled or expired
        }
        if (!booking.getbookingTime().isBefore(LocalDateTime.now(clock).minusMinutes(graceMinutes))) {
            return;
        }

        releaseSlot(booking.getstationId());
        booking.setbookingStatus(BookingStatus.EXPIRED);


        int ownerId = access.ownerOfBooking(booking);

        Booking savedBooking = bookingRepository.save(booking);

        inAppNotificationService.createNotification(ownerId, savedBooking.getbookingId(), "Booking Expired", "Your booking #" + savedBooking.getbookingId() + " has expired because charging was not started within the allowed time.", "BOOKING_EXPIRED");
    }

    /**
     * Must be called inside a transaction. Frees one slot of the station (never above total_slots).
     */
    void releaseSlot(int stationId) {
        chargingStationRepository.findByIdForUpdate(stationId).ifPresent(station -> {
            if (station.getavailableSlots() < station.gettotalSlots()) {
                station.setavailableSlots(station.getavailableSlots() + 1);
                chargingStationRepository.save(station);
            }
        });
    }
}
