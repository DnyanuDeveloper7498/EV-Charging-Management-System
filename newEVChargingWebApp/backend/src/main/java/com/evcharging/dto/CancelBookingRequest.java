package com.evcharging.dto;

import jakarta.validation.constraints.Size;

public class CancelBookingRequest {


    @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
    private String cancellationReason;

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
