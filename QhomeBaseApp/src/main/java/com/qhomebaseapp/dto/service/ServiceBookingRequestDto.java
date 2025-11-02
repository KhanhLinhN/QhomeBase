package com.qhomebaseapp.dto.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceBookingRequestDto {
    
    @NotNull(message = "Service ID is required")
    private Long serviceId;
    
    @NotNull(message = "Booking date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;
    
    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    
    @Min(value = 1, message = "Number of people must be at least 1")
    @NotNull(message = "Number of people is required")
    private Integer numberOfPeople;
    
    private String purpose;
    
    @NotNull(message = "Terms must be accepted")
    private Boolean termsAccepted;
}

