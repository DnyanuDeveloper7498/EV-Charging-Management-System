package com.evcharging.service;

import com.evcharging.dto.StationRequest;
import com.evcharging.exception.ChargingStationNotFoundException;
import com.evcharging.model.ChargingStation;
import com.evcharging.model.Roles;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.ChargingStationRepository;
import com.evcharging.security.AuthUser;
import com.evcharging.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChargingStationService {

    private final ChargingStationRepository chargingStationRepository;
    private final BookingRepository bookingRepository;

    public ChargingStationService(ChargingStationRepository chargingStationRepository, BookingRepository bookingRepository) {
        this.chargingStationRepository = chargingStationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public ChargingStation addStation(StationRequest request) {
        ChargingStation station = new ChargingStation();
        apply(station, request);
        station.setavailableSlots(request.totalSlots());
        station.setOwnerId(SecurityUtils.currentUserId());
        return chargingStationRepository.save(station);
    }

    /**
     * ADMIN can edit any station, a STATION_OWNER only the stations he created. Total slots can be changed, but
     * never below the number of slots currently in use.
     */
    @Transactional
    public ChargingStation updateStation(int stationId, StationRequest request) {
        ChargingStation station = chargingStationRepository.findByIdForUpdate(stationId).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));
        requireCanManage(station);

        int inUse = station.gettotalSlots() - station.getavailableSlots();
        if (request.totalSlots() < inUse) {
            throw new IllegalArgumentException("Total slots cannot be lower than the " + inUse + " slot(s) currently in use.");
        }

        boolean keepStatus = request.status() == null || request.status().isBlank();
        String oldStatus = station.getStatus();

        apply(station, request);
        if (keepStatus) {
            station.setStatus(oldStatus);
        }
        station.setavailableSlots(request.totalSlots() - inUse);
        return chargingStationRepository.save(station);
    }

    @Transactional
    public void deleteStation(int stationId) {
        ChargingStation station = chargingStationRepository.findByIdForUpdate(stationId).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));
        requireCanManage(station);

        if (bookingRepository.existsByStationId(stationId)) {
            throw new IllegalStateException("This station has booking history and cannot be deleted. Set its status to OFFLINE instead.");
        }
        chargingStationRepository.delete(station);
    }

    public ChargingStation findByStationId(int stationId) {
        return chargingStationRepository.findById(stationId).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));
    }

    public List<ChargingStation> getAllStations() {
        return chargingStationRepository.findAll();
    }

    /**
     * Admin override of the free-slot counter. Bookings and sessions normally maintain it automatically.
     */
    @Transactional
    public void updateAvailableSlots(int stationId, int availableSlots) {
        ChargingStation station = chargingStationRepository.findByIdForUpdate(stationId).orElseThrow(() -> new ChargingStationNotFoundException("Charging station not found."));

        if (availableSlots < 0 || availableSlots > station.gettotalSlots()) {
            throw new IllegalArgumentException("Available slots must be between 0 and " + station.gettotalSlots() + ".");
        }

        station.setavailableSlots(availableSlots);
        chargingStationRepository.save(station);
    }

    private static void apply(ChargingStation station, StationRequest request) {
        station.setstationName(request.stationName().trim());
        station.setsLocation(request.sLocation().trim());
        station.settotalSlots(request.totalSlots());
        station.setpricePerKwt(request.pricePerKwt());
        station.setchargerType(request.chargerType().trim());
        station.setStatus(request.status() == null || request.status().isBlank() ? "AVAILABLE" : request.status().trim().toUpperCase());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
    }

    private static void requireCanManage(ChargingStation station) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        AuthUser user = SecurityUtils.currentUser();
        // Station Admin can manage every station (edit); delete is blocked in SecurityConfig
        if (Roles.STATION_ADMIN.equals(user.role())) {
            return;
        }
        boolean owner = Roles.STATION_OWNER.equals(user.role()) && station.getOwnerId() != null && station.getOwnerId().intValue() == user.userId();
        if (!owner) {
            throw new AccessDeniedException("You can only manage your own stations.");
        }
    }
}
