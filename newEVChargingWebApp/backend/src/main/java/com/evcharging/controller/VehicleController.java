package com.evcharging.controller;

import com.evcharging.dto.CreateVehicleRequest;
import com.evcharging.model.Vehicle;
import com.evcharging.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<Vehicle> addVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.vehicleAdd(request));
    }

    @GetMapping("/{vehicleId}")
    public Vehicle getVehicleById(@PathVariable int vehicleId) {
        return vehicleService.findByVehicleId(vehicleId);
    }

    @GetMapping("/number/{vehicleNumber}")
    public Vehicle getVehicleByNumber(@PathVariable String vehicleNumber) {
        return vehicleService.findByVehicleNumber(vehicleNumber);
    }

    @GetMapping("/user/{userId}")
    public List<Vehicle> getVehiclesByUser(@PathVariable int userId) {
        return vehicleService.findByUserId(userId);
    }

    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleService.getAllVehicles();
    }

    /** PUT /api/vehicles/{id}/battery?batteryPercentage=55 (the frontend now uses exactly this route). */
    @PutMapping("/{vehicleId}/battery")
    public ResponseEntity<Void> updateBattery(@PathVariable int vehicleId,
                                              @RequestParam double batteryPercentage) {
        vehicleService.updateBatteryPercentage(vehicleId, batteryPercentage);
        return ResponseEntity.ok().build();
    }
}
