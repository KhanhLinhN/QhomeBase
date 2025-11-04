package com.qhomebaseapp.exception;

/**
 * Exception thrown when a service is not available for the selected time slot.
 * This could be due to:
 * - No schedule available for the selected day/time
 * - Time slot already booked by another user
 * - Service is outside operating hours
 */
public class ServiceNotAvailableException extends RuntimeException {
    
    private final String reason;
    
    public ServiceNotAvailableException(String message) {
        super(message);
        this.reason = null;
    }
    
    public ServiceNotAvailableException(String message, String reason) {
        super(message);
        this.reason = reason;
    }
    
    public ServiceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
        this.reason = null;
    }
    
    public String getReason() {
        return reason;
    }
}

