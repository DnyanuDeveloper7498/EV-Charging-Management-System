package com.evcharging.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TopupFailureRequest(@NotBlank @JsonProperty("razorpay_order_id") String razorpayOrderId,

                                  @NotBlank @JsonProperty("razorpay_payment_id") String razorpayPaymentId) {
}