package com.evcharging.dto;

import jakarta.validation.constraints.*;

public record CreateVehicleRequest(
        @NotBlank(message = "Brand is required.")
        @Size(max = 80, message = "Brand must be at most 80 characters.")
        String brand,

        @NotBlank(message = "Model is required.")
        @Size(max = 80, message = "Model must be at most 80 characters.")
        String model,

        @NotBlank(message = "Vehicle type is required.")
        @Size(max = 40, message = "Vehicle type must be at most 40 characters.")
        String vehicleType,

        @NotBlank(message = "Vehicle number is required.")
        @Pattern(regexp = "^[A-Za-z0-9 -]{4,20}$", message = "Enter a valid vehicle number.")
        String vehicleNumber,

        @NotNull(message = "Battery capacity is required.")
        @DecimalMin(value = "0.1", message = "Battery capacity must be greater than 0.")
        @DecimalMax(value = "1000", message = "Battery capacity is too large.")
        Double batteryCapacity,

        @NotNull(message = "Battery percentage is required.")
        @DecimalMin(value = "0", message = "Battery percentage must be between 0 and 100.")
        @DecimalMax(value = "100", message = "Battery percentage must be between 0 and 100.")
        Double batteryPercentage) {
}
