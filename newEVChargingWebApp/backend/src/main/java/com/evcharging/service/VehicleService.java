package com.evcharging.service;

import com.evcharging.dto.CreateVehicleRequest;
import com.evcharging.exception.DuplicateResourceException;
import com.evcharging.exception.VehicleNotFoundException;
import com.evcharging.model.BookingStatus;
import com.evcharging.model.Vehicle;
import com.evcharging.repository.BookingRepository;
import com.evcharging.repository.VehicleRepository;
import com.evcharging.security.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final AccessService access;

    public VehicleService(VehicleRepository vehicleRepository, BookingRepository bookingRepository,
                          AccessService access) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.access = access;
    }

    /** The owner is ALWAYS the logged-in user; a userId in the request body is ignored. */
    @Transactional
    public Vehicle vehicleAdd(CreateVehicleRequest request) {
        String number = normalizeNumber(request.vehicleNumber());

        if (vehicleRepository.existsByVehicleNumber(number)) {
            throw new DuplicateResourceException("A vehicle with this number is already registered.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(SecurityUtils.currentUserId());
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setVehicleType(request.vehicleType().trim());
        vehicle.setVehicleNumber(number);
        vehicle.setBatteryCapacity(request.batteryCapacity());
        vehicle.setBatteryPercentage(request.batteryPercentage());

        try {
            return vehicleRepository.saveAndFlush(vehicle);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("A vehicle with this number is already registered.");
        }
    }

    public Vehicle findByVehicleNumber(String vehicleNumber) {
        Vehicle vehicle = vehicleRepository.findByVehicleNumber(normalizeNumber(vehicleNumber))
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));


        if (!SecurityUtils.isSelfOrAdminOrVehicleAdmin(vehicle.getUserId()))  {
            throw new VehicleNotFoundException("Vehicle not found.");
        }
        return vehicle;
    }

    public Vehicle findByVehicleId(int vehicleId) {
        return access.vehicleForCurrentUserOrAdmin(vehicleId);
    }

    @Transactional
    public void updateBatteryPercentage(int vehicleId, double batteryPercentage) {
        if (Double.isNaN(batteryPercentage) || batteryPercentage < 0 || batteryPercentage > 100) {
            throw new IllegalArgumentException("Battery percentage must be between 0 and 100.");
        }

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not found."));

        if (!SecurityUtils.isSelfOrAdmin(vehicle.getUserId())) {
            throw new VehicleNotFoundException("Vehicle not found.");
        }

        if (bookingRepository.countByVehicleIdAndBookingStatus(vehicleId, BookingStatus.IN_PROGRESS) > 0) {
            throw new IllegalStateException("The battery cannot be edited while this vehicle is charging.");
        }

        vehicle.setBatteryPercentage(batteryPercentage);
        vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        SecurityUtils.requireAdminOrVehicleAdmin();
        return vehicleRepository.findAll();
    }

    public List<Vehicle> findByUserId(int userId) {
        if (!SecurityUtils.isSelfOrAdminOrVehicleAdmin(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only access your own data.");
        }
        return vehicleRepository.findByUserId(userId);
    }

    private static String normalizeNumber(String value) {
        return value == null ? "" : value.trim().toUpperCase().replaceAll("\\s+", "");
    }
}
