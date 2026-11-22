package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TopupVerifyRequest(
        @NotBlank(message = "Missing payment verification details.") @JsonProperty("razorpay_order_id") String razorpayOrderId,
        @NotBlank(message = "Missing payment verification details.") @JsonProperty("razorpay_payment_id") String razorpayPaymentId,
        @NotBlank(message = "Missing payment verification details.") @JsonProperty("razorpay_signature") String razorpaySignature) {
}
