package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyPaymentRequest(
        @NotNull(message = "Session is required.") Integer sessionId,
        @NotBlank(message = "Missing payment verification details.") @JsonProperty("razorpay_payment_id") String razorpayPaymentId,
        @NotBlank(message = "Missing payment verification details.") @JsonProperty("razorpay_signature") String razorpaySignature) {
}
