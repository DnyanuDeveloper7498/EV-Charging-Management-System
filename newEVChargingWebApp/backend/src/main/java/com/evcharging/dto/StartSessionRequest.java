package com.evcharging.dto;

import jakarta.validation.constraints.NotNull;

/** The start time is always taken from the server clock, so the client cannot send one. */
public record StartSessionRequest(@NotNull(message = "Booking is required.") Integer bookingId) {
}
