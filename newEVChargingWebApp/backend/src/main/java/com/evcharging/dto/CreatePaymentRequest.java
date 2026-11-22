package com.evcharging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "Session is required.") Integer sessionId,
        @NotBlank(message = "Payment method is required.") String paymentMethod) {
}
