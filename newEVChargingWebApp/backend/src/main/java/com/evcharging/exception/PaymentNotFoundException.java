package com.evcharging.exception;

public class PaymentNotFoundException  extends  RuntimeException{

    public PaymentNotFoundException(String message)
    {
        super(message);
    }
}
