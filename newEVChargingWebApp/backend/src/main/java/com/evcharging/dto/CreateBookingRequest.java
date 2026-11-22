package com.evcharging.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateBookingRequest(
        @NotNull(message = "Vehicle is required.") Integer vehicleId,
        @NotNull(message = "Station is required.") Integer stationId,
        @NotNull(message = "Booking time is required.") LocalDateTime bookingTime) {
}
