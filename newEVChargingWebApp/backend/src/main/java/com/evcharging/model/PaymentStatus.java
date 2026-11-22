package com.evcharging.model;

/** Payment lifecycle: PENDING -> SUCCESS, or PENDING -> FAILED -> (retry) PENDING -> SUCCESS. */
public final class PaymentStatus {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private PaymentStatus() {
    }
}
