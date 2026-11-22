package com.evcharging.exception;

public class BookingNotFoundException  extends RuntimeException{

    public BookingNotFoundException(String message)
    {
        super(message);
    }
}
