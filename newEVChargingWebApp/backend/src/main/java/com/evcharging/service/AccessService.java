package com.evcharging.service;

import com.evcharging.exception.BookingNotFoundException;
import com.evcharging.exception.ChargingSessionNotFoundException;
import com.evcharging.exception.VehicleNotFoundException;
import com.evcharging.model.Booking;
import com.evcharging.model.ChargingSession;
import com.evcharging.model.Payment;
import com.evcharging.model.Vehicle;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingSessionRepository;
import com.evcharging.repository.VehicleRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * Central ownership checks. A resource that belongs to someone else is reported as "not found"
 * (instead of "forbidden") so that ids cannot be probed.
 */
@Service
public class AccessService {

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final ChargingSessionRepository sessionRepository;

    public AccessService(VehicleRepository vehicleRepository,
                         BookingRepository bookingRepository,
                         ChargingSessionRepository sessionRepository) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
    }

    /** Id of the user that owns the booking's vehicle, or -1 if the vehicle no longer exists. */
    public int ownerOfBooking(Booking booking) {
        return vehicleRepository.findById(booking.getvehicleId()).map(Vehicle::getUserId).orElse(-1);
    }

    public int ownerOfSession(ChargingSession session) {
        return bookingRepository.findById(session.getbookingId()).map(this::ownerOfBooking).orElse(-1);
    }

    public Vehicle vehicleForCurrentUserOrAdmin(int vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));
        if (!SecurityUtils.isSelfOrAdmin(vehicle.getUserId())) {
            throw new VehicleNotFoundException("Vehicle not found.");
        }
        return vehicle;
    }

    public Booking bookingForCurrentUserOrAdmin(int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));
        if (!SecurityUtils.isSelfOrAdmin(ownerOfBooking(booking))) {
            throw new BookingNotFoundException("Booking not found.");
        }
        return booking;
    }

    public ChargingSession sessionForCurrentUserOrAdmin(int sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChargingSessionNotFoundException("Charging session not found."));
        if (!SecurityUtils.isSelfOrAdmin(ownerOfSession(session))) {
            throw new ChargingSessionNotFoundException("Charging session not found.");
        }
        return session;
    }

    /** Strict owner check (admins are NOT allowed) - used for paying, because only the owner pays. */
    public ChargingSession sessionOwnedByCurrentUser(int sessionId) {
        ChargingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChargingSessionNotFoundException("Charging session not found."));
        if (ownerOfSession(session) != SecurityUtils.currentUserId()) {
            throw new ChargingSessionNotFoundException("Charging session not found.");
        }
        return session;
    }

    public void requirePaymentVisible(Payment payment) {
        ChargingSession session = sessionRepository.findById(payment.getsessionId()).orElse(null);
        if (session == null || !SecurityUtils.isSelfOrAdmin(ownerOfSession(session))) {
            throw new ChargingSessionNotFoundException("Payment not found.");
        }
    }
}
