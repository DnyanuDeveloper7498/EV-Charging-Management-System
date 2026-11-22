package com.evcharging.service;

import com.evcharging.dto.ReportSummary;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingSessionRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.repository.PaymentRepository;
import com.evcharging.repository.UserRepository;
import com.evcharging.repository.VehicleRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admins get the system-wide summary; every other user gets a personal summary built only from their own data,
 * so customers can never see other customers' numbers.
 */
@Service
public class ReportService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ChargingStationRepository stationRepository;
    private final BookingRepository bookingRepository;
    private final ChargingSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;

    public ReportService(UserRepository userRepository, VehicleRepository vehicleRepository,
                         ChargingStationRepository stationRepository, BookingRepository bookingRepository,
                         ChargingSessionRepository sessionRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.bookingRepository = bookingRepository;
        this.sessionRepository = sessionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummary summary() {
        if (SecurityUtils.isAdmin()) {
            return new ReportSummary(
                    "SYSTEM",
                    userRepository.count(),
                    vehicleRepository.count(),
                    stationRepository.count(),
                    bookingRepository.count(),
                    sessionRepository.count(),
                    sessionRepository.sumUnitsConsumed(),
                    paymentRepository.sumSuccessfulAmount(),
                    null);
        }

        int userId = SecurityUtils.currentUserId();
        return new ReportSummary(
                "PERSONAL",
                null,
                vehicleRepository.countByUserId(userId),
                null,
                bookingRepository.countByUserId(userId),
                sessionRepository.countByUser(userId),
                sessionRepository.sumUnitsConsumedByUser(userId),
                null,
                paymentRepository.sumSuccessfulAmountByUser(userId));
    }
}
