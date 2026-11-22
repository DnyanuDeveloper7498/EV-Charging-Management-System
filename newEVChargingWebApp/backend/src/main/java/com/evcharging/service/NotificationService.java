package com.evcharging.service;

import com.evcharging.model.Booking;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.User;
import com.evcharging.model.Vehicle;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.UserRepository;
import com.evcharging.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/** E-mail notifications. Runs in the background (@Async) so it never slows down or fails the API call. */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final EmailService emailService;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ChargingStationRepository stationRepository;

    public NotificationService(EmailService emailService, VehicleRepository vehicleRepository,
                               UserRepository userRepository, ChargingStationRepository stationRepository) {
        this.emailService = emailService;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.stationRepository = stationRepository;
    }

    @Async
    public void bookingConfirmed(Booking booking) {
        try {
            Vehicle vehicle = vehicleRepository.findById(booking.getvehicleId()).orElse(null);
            ChargingStation station = stationRepository.findById(booking.getstationId()).orElse(null);
            if (vehicle == null || station == null) {
                return;
            }
            User user = userRepository.findById(vehicle.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null) {
                return;
            }

            String body = "Hi " + user.getFullName() + ",\n\n"
                    + "Your charging slot is booked.\n\n"
                    + "Booking ID : #" + booking.getbookingId() + "\n"
                    + "Station    : " + station.getstationName() + " (" + station.getsLocation() + ")\n"
                    + "Vehicle    : " + vehicle.getVehicleNumber() + "\n"
                    + "Time       : " + booking.getbookingTime().format(WHEN) + "\n\n"
                    + "Please arrive on time - unused bookings expire automatically.\n\n"
                    + "Smart EV Charging";
            emailService.send(user.getEmail(), "Booking confirmed - #" + booking.getbookingId(), body);
        } catch (Exception e) {
            log.warn("Booking notification failed: {}", e.getMessage());
        }
    }
}
