package com.evcharging.service;

import com.evcharging.dto.StartSessionRequest;
import com.evcharging.exception.BookingNotFoundException;
import com.evcharging.exception.ChargingSessionNotFoundException;
import com.evcharging.exception.ChargingStationNotFoundException;
import com.evcharging.exception.VehicleNotFoundException;
import com.evcharging.model.Booking;
import com.evcharging.model.BookingStatus;
import com.evcharging.model.ChargingSession;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.Vehicle;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingSessionRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.VehicleRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChargingSessionService {

    private final ChargingSessionRepository chargingSessionRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final AccessService access;
    private final PaymentService paymentService;
    private final Clock clock;

    private final long earlyStartMinutes;
    private final long graceMinutes;

    private final InAppNotificationService inAppNotificationService;

    public ChargingSessionService(ChargingSessionRepository chargingSessionRepository,
                                  BookingRepository bookingRepository,
                                  VehicleRepository vehicleRepository,
                                  ChargingStationRepository chargingStationRepository,
                                  AccessService access,
                                  PaymentService paymentService,
                                  InAppNotificationService inAppNotificationService,
                                  Clock clock,
                                  @Value("${app.booking.early-start-minutes}") long earlyStartMinutes,
                                  @Value("${app.booking.grace-minutes}") long graceMinutes) {
        this.chargingSessionRepository = chargingSessionRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.chargingStationRepository = chargingStationRepository;
        this.access = access;
        this.paymentService = paymentService;
        this.inAppNotificationService=inAppNotificationService;
        this.clock = clock;
        this.earlyStartMinutes = earlyStartMinutes;
        this.graceMinutes = graceMinutes;
    }

    // ------------------------------------------------------------------ reads

    public List<ChargingSession> getSessionsForUser(int userId) {
        SecurityUtils.requireSelfOrAdmin(userId);
        return chargingSessionRepository.findByUserId(userId);
    }

    public List<ChargingSession> getAllSessions() {
        SecurityUtils.requireAdminOrChargingAdmin();
        return chargingSessionRepository.findAll();
    }

    public ChargingSession getSessionById(int sessionId) {
        return access.sessionForCurrentUserOrAdmin(sessionId);
    }

    public ChargingSession getSessionByBookingId(int bookingId) {
        access.bookingForCurrentUserOrAdmin(bookingId);
        return chargingSessionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ChargingSessionNotFoundException("Charging session not found."));
    }

    public List<ChargingSession> getActiveSessions() {
        SecurityUtils.requireAdminOrChargingAdmin();
        return chargingSessionRepository.findByEndTimeIsNull();
    }

    // ------------------------------------------------------------------ start

    @Transactional
    public ChargingSession startSession(StartSessionRequest request) {
        int userId = SecurityUtils.currentUserId();

        Booking booking = bookingRepository.findByIdForUpdate(request.bookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        Vehicle vehicle = vehicleRepository.findById(booking.getvehicleId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));

        // a booking can only be started by the owner of its vehicle
        if (vehicle.getUserId() != userId) {
            throw new BookingNotFoundException("Booking not found.");
        }

        if (!BookingStatus.CONFIRMED.equalsIgnoreCase(booking.getbookingStatus())) {
            throw new IllegalStateException("Only confirmed bookings can start charging.");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (now.isBefore(booking.getbookingTime().minusMinutes(earlyStartMinutes))) {
            throw new IllegalStateException("Charging cannot be started before the booking time ("
                    + booking.getbookingTime() + ").");
        }
        if (now.isAfter(booking.getbookingTime().plusMinutes(graceMinutes))) {
            throw new IllegalStateException("This booking has expired. Please create a new booking.");
        }

        // the unique constraint on charging_sessions.booking_id is the last line of defence
        if (chargingSessionRepository.findByBookingId(booking.getbookingId()).isPresent()) {
            throw new IllegalStateException("A charging session already exists for this booking.");
        }
        if (bookingRepository.countByVehicleIdAndBookingStatus(vehicle.getVehicleId(), BookingStatus.IN_PROGRESS) > 0) {
            throw new IllegalStateException("This vehicle is already charging.");
        }

        ChargingSession session = new ChargingSession();
        session.setbookingId(booking.getbookingId());
        session.setstartTime(now);                                   // always server time
        session.setStartBatteryPercentage(vehicle.getBatteryPercentage());
        session.setEndBatteryPercentage(0);
        session.setunitsConsumed(BigDecimal.ZERO);
        session.settotalCost(BigDecimal.ZERO);

        // the station slot was reserved when the booking was created and stays reserved until the session stops
        booking.setbookingStatus(BookingStatus.IN_PROGRESS);
        bookingRepository.save(booking);

        ChargingSession savedSession= chargingSessionRepository.save(session);

        inAppNotificationService.createNotification(userId,booking.getbookingId(),"Charging Started",
                "Charging has started for booking #" + booking.getbookingId()
                        + ". Battery at " + (int) vehicle.getBatteryPercentage() + "%.",
                "CHARGING_STARTED");
        return  savedSession;
    }

    // ------------------------------------------------------------------- stop

    /**
     * Stops the session, calculates the bill, frees the slot and creates the (PENDING) payment record.
     * The customer then pays that one record either from the wallet or through Razorpay.
     */
    @Transactional
    public ChargingSession stopSession(int sessionId, double endBatteryPercentage) {
        if (Double.isNaN(endBatteryPercentage) || endBatteryPercentage < 0 || endBatteryPercentage > 100) {
            throw new IllegalArgumentException("Battery percentage must be between 0 and 100.");
        }

        // lock order: session -> booking -> vehicle -> station
        ChargingSession session = chargingSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ChargingSessionNotFoundException("Charging session not found."));

        Booking booking = bookingRepository.findByIdForUpdate(session.getbookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(booking.getvehicleId())
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));

        if (!SecurityUtils.isSelfOrAdmin(vehicle.getUserId())) {
            throw new ChargingSessionNotFoundException("Charging session not found.");
        }

        if (session.getendTime() != null) {
            return session; // already stopped - idempotent for double clicks / retries
        }

        double startBattery = session.getStartBatteryPercentage();
        if (endBatteryPercentage < startBattery) {
            throw new IllegalArgumentException("End battery percentage cannot be less than start battery percentage.");
        }

        ChargingStation station = chargingStationRepository.findByIdForUpdate(booking.getstationId())
                .orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));

        BigDecimal percentGained = BigDecimal.valueOf(endBatteryPercentage).subtract(BigDecimal.valueOf(startBattery));
        BigDecimal unitsConsumed = BigDecimal.valueOf(vehicle.getBatteryCapacity())
                .multiply(percentGained)
                .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
        BigDecimal totalCost = unitsConsumed.multiply(station.getpricePerKwt()).setScale(2, RoundingMode.HALF_UP);

        session.setendTime(LocalDateTime.now(clock));
        session.setEndBatteryPercentage(endBatteryPercentage);
        session.setunitsConsumed(unitsConsumed);
        session.settotalCost(totalCost);
        ChargingSession saved = chargingSessionRepository.save(session);

        vehicle.setBatteryPercentage(endBatteryPercentage);
        vehicleRepository.save(vehicle);

        if (station.getavailableSlots() < station.gettotalSlots()) {
            station.setavailableSlots(station.getavailableSlots() + 1);
            chargingStationRepository.save(station);
        }

        booking.setbookingStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        paymentService.createPendingPayment(saved);

        inAppNotificationService.createNotification( vehicle.getUserId(),
                booking.getbookingId(),
                "Charging Complete",
                "Charging finished for booking #" + booking.getbookingId()
                        + ". Units: " + unitsConsumed + " kWh, Total: ₹" + totalCost
                        + ". Please complete the payment.",
                "CHARGING_COMPLETED");

        return saved;
    }
}
