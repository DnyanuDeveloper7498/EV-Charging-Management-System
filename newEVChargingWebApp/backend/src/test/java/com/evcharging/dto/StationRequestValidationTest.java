package com.evcharging.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private StationRequest request(int slots, BigDecimal price, Double lat, Double lng) {
        return new StationRequest("City Charge", "MG Road, Pune", slots, price, "DC Fast", "AVAILABLE", lat, lng);
    }

    @Test
    void validStationHasNoViolations() {
        assertTrue(validator.validate(request(6, new BigDecimal("12.50"), 18.52, 73.85)).isEmpty());
    }

    @Test
    void coordinatesAreOptional() {
        assertTrue(validator.validate(request(6, new BigDecimal("12.50"), null, null)).isEmpty());
    }

    @Test
    void outOfRangeCoordinatesAreRejected() {
        assertEquals(1, validator.validate(request(6, new BigDecimal("12.50"), 120.0, 73.85)).size());
        assertEquals(1, validator.validate(request(6, new BigDecimal("12.50"), 18.5, 200.0)).size());
    }

    @Test
    void zeroSlotsAndNegativePriceAreRejected() {
        assertEquals(1, validator.validate(request(0, new BigDecimal("12.50"), null, null)).size());
        assertEquals(1, validator.validate(request(5, new BigDecimal("-1"), null, null)).size());
    }
}
