package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record StationRequest(
        @NotBlank(message = "Station name is required.")
        @Size(max = 150, message = "Station name must be at most 150 characters.")
        String stationName,

        @NotBlank(message = "Location is required.")
        @Size(max = 250, message = "Location must be at most 250 characters.")
        @JsonProperty("sLocation")
        String sLocation,

        @Min(value = 1, message = "A station needs at least 1 slot.")
        @Max(value = 1000, message = "Too many slots.")
        int totalSlots,

        @NotNull(message = "Price per kWh is required.")
        @DecimalMin(value = "0", message = "Price cannot be negative.")
        @DecimalMax(value = "100000", message = "Price is too large.")
        BigDecimal pricePerKwt,

        @NotBlank(message = "Charger type is required.")
        @Size(max = 50, message = "Charger type must be at most 50 characters.")
        String chargerType,

        @Size(max = 30, message = "Status must be at most 30 characters.")
        String status,

        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90.")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90.")
        Double latitude,

        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180.")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180.")
        Double longitude) {
}
