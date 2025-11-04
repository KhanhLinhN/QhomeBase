package com.qhomebaseapp.exception;

/**
 * Exception thrown when user tries to create a new booking
 * but has unpaid bookings that need to be paid first.
 */
public class UnpaidBookingException extends RuntimeException {
    
    public UnpaidBookingException(String message) {
        super(message);
    }
    
    public UnpaidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}

