package com.evcharging.exception;

/** Razorpay could not be reached / returned an error. Maps to HTTP 502. */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentGatewayException(String message) {
        super(message);
    }
}
