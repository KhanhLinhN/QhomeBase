package com.qhomebaseapp.service.service;

import com.qhomebaseapp.dto.service.AvailableServiceDto;
import com.qhomebaseapp.dto.service.ServiceBookingRequestDto;
import com.qhomebaseapp.dto.service.ServiceBookingResponseDto;
import com.qhomebaseapp.dto.service.ServiceDto;
import com.qhomebaseapp.dto.service.ServiceTypeDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ServiceBookingService {
    
    List<ServiceDto> getServicesByCategory(Long categoryId);
    
    List<ServiceDto> getServicesByCategoryCode(String categoryCode);
    
    List<ServiceTypeDto> getServiceTypesByCategoryCode(String categoryCode);
    
    List<ServiceDto> getServicesByCategoryCodeAndType(String categoryCode, String serviceType);
    
    ServiceDto getServiceById(Long serviceId);
    
    List<AvailableServiceDto> getAvailableServices(
        Long categoryId, 
        LocalDate date, 
        LocalTime startTime, 
        LocalTime endTime
    );
    
    List<AvailableServiceDto> getAvailableServicesByCategoryCode(
        String categoryCode,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    );
    
    List<AvailableServiceDto> getAvailableServicesByCategoryCodeAndType(
        String categoryCode,
        String serviceType,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    );
    
    ServiceBookingResponseDto createBooking(ServiceBookingRequestDto request, Long userId);
    
    List<ServiceBookingResponseDto> getUserBookings(Long userId);
    
    ServiceBookingResponseDto getBookingById(Long bookingId, Long userId);
    
    String createVnpayPaymentUrl(Long bookingId, Long userId, jakarta.servlet.http.HttpServletRequest request);
    
    void handleVnpayCallback(Long bookingId, String transactionRef, java.time.OffsetDateTime paymentDate, String userEmail);
    
    List<ServiceBookingResponseDto> getUnpaidBookings(Long userId);
    
    void cancelExpiredUnpaidBookings();
}

