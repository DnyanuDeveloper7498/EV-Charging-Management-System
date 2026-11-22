package com.evcharging.exception;

/** Thrown when a unique business value (e-mail, vehicle number ...) already exists. Maps to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
