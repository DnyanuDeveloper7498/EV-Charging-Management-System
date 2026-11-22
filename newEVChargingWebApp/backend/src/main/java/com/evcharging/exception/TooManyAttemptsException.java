package com.evcharging.exception;

/** Thrown when too many failed logins were made. Maps to HTTP 429. */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
