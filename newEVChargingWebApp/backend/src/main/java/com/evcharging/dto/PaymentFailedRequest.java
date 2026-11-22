package com.evcharging.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentFailedRequest(
        @NotNull(message = "Session is required.") Integer sessionId,
        @Size(max = 255, message = "Reason is too long.") String reason) {
}
