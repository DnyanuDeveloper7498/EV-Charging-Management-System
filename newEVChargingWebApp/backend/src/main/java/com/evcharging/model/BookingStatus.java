package com.evcharging.model;

import java.util.List;

public final class BookingStatus {

    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String EXPIRED = "EXPIRED";
    public static final String REJECTED = "REJECTED";

    public static final List<String> ACTIVE = List.of(
            PENDING_APPROVAL,
            CONFIRMED,
            IN_PROGRESS
    );

    private BookingStatus() {
    }
}