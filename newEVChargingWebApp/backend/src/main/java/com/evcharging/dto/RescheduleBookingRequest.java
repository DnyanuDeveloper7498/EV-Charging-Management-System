package com.evcharging.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class RescheduleBookingRequest {

    @NotNull(message = "New booking time is required")
    private LocalDateTime bookingTime;

    public LocalDateTime getBookingTime()
    {
        return bookingTime;
    }

    public void  setBookingTime(LocalDateTime bookingTime)
    {
        this.bookingTime=bookingTime;
    }


}
