package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.service.AvailableServiceDto;
import com.qhomebaseapp.dto.service.BarSlotDto;
import com.qhomebaseapp.dto.service.ServiceBookingRequestDto;
import com.qhomebaseapp.dto.service.ServiceBookingResponseDto;
import com.qhomebaseapp.dto.service.ServiceComboDto;
import com.qhomebaseapp.dto.service.ServiceDto;
import com.qhomebaseapp.dto.service.ServiceOptionDto;
import com.qhomebaseapp.dto.service.ServiceTicketDto;
import com.qhomebaseapp.dto.service.ServiceTypeDto;
import com.qhomebaseapp.dto.service.TimeSlotDto;
import com.qhomebaseapp.service.service.ServiceBookingService;
import com.qhomebaseapp.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/service-booking")
@RequiredArgsConstructor
public class ServiceBookingController {

    private final ServiceBookingService serviceBookingService;

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails customUser) {
            return customUser.getUserId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found in authentication");
    }

    @GetMapping("/categories/{categoryId}/services")
    public ResponseEntity<List<ServiceDto>> getServicesByCategory(@PathVariable Long categoryId) {
        List<ServiceDto> services = serviceBookingService.getServicesByCategory(categoryId);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/categories/code/{categoryCode}/services")
    public ResponseEntity<List<ServiceDto>> getServicesByCategoryCode(@PathVariable String categoryCode) {
        List<ServiceDto> services = serviceBookingService.getServicesByCategoryCode(categoryCode);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/categories/code/{categoryCode}/service-types")
    public ResponseEntity<List<ServiceTypeDto>> getServiceTypesByCategoryCode(@PathVariable String categoryCode) {
        List<ServiceTypeDto> serviceTypes = serviceBookingService.getServiceTypesByCategoryCode(categoryCode);
        return ResponseEntity.ok(serviceTypes);
    }

    @GetMapping("/categories/code/{categoryCode}/services/type/{serviceType}")
    public ResponseEntity<List<ServiceDto>> getServicesByCategoryCodeAndType(
            @PathVariable String categoryCode,
            @PathVariable String serviceType) {
        List<ServiceDto> services = serviceBookingService.getServicesByCategoryCodeAndType(categoryCode, serviceType);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/services/{serviceId}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable Long serviceId) {
        ServiceDto service = serviceBookingService.getServiceById(serviceId);
        return ResponseEntity.ok(service);
    }

    @GetMapping("/available")
    public ResponseEntity<List<AvailableServiceDto>> getAvailableServices(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String serviceType,
            @RequestParam LocalDate date,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        
        List<AvailableServiceDto> available;
        if (categoryCode != null) {
            // If serviceType is provided, filter by both category and type
            if (serviceType != null && !serviceType.isEmpty()) {
                available = serviceBookingService.getAvailableServicesByCategoryCodeAndType(
                        categoryCode, serviceType, date, startTime, endTime);
            } else {
                // Get available services by category code only
                available = serviceBookingService.getAvailableServicesByCategoryCode(
                        categoryCode, date, startTime, endTime);
            }
        } else if (categoryId != null) {
            available = serviceBookingService.getAvailableServices(
                    categoryId, date, startTime, endTime);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(available);
    }

    @PostMapping("/book")
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody ServiceBookingRequestDto request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            ServiceBookingResponseDto booking = serviceBookingService.createBooking(request, userId);
            return ResponseEntity.ok(booking);
        } catch (com.qhomebaseapp.exception.UnpaidBookingException | 
                 com.qhomebaseapp.exception.ServiceNotAvailableException ex) {
            // These exceptions will be handled by GlobalExceptionHandler
            // which will return 400 Bad Request with clear message
            throw ex;
        } catch (Exception ex) {
            log.error("Error creating booking: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + ex.getMessage()
            ));
        }
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<ServiceBookingResponseDto>> getMyBookings(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ServiceBookingResponseDto> bookings = serviceBookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/my-bookings/{bookingId}")
    public ResponseEntity<ServiceBookingResponseDto> getMyBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        ServiceBookingResponseDto booking = serviceBookingService.getBookingById(bookingId, userId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/unpaid")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceBookingResponseDto>> getUnpaidBookings(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ServiceBookingResponseDto> unpaidBookings = serviceBookingService.getUnpaidBookings(userId);
        return ResponseEntity.ok(unpaidBookings);
    }
    
    @GetMapping("/services/{serviceId}/time-slots")
    public ResponseEntity<List<TimeSlotDto>> getTimeSlotsForService(
            @PathVariable Long serviceId,
            @RequestParam LocalDate date) {
        List<TimeSlotDto> timeSlots = serviceBookingService.getTimeSlotsForService(serviceId, date);
        return ResponseEntity.ok(timeSlots);
    }
    
    @GetMapping("/services/{serviceId}/options")
    public ResponseEntity<List<ServiceOptionDto>> getServiceOptions(@PathVariable Long serviceId) {
        List<ServiceOptionDto> options = serviceBookingService.getServiceOptions(serviceId);
        return ResponseEntity.ok(options);
    }
    
    @GetMapping("/services/{serviceId}/combos")
    public ResponseEntity<List<ServiceComboDto>> getServiceCombos(@PathVariable Long serviceId) {
        List<ServiceComboDto> combos = serviceBookingService.getServiceCombos(serviceId);
        return ResponseEntity.ok(combos);
    }
    
    @GetMapping("/services/{serviceId}/tickets")
    public ResponseEntity<List<ServiceTicketDto>> getServiceTickets(@PathVariable Long serviceId) {
        List<ServiceTicketDto> tickets = serviceBookingService.getServiceTickets(serviceId);
        return ResponseEntity.ok(tickets);
    }
    
    @GetMapping("/services/{serviceId}/bar-slots")
    public ResponseEntity<List<BarSlotDto>> getBarSlots(@PathVariable Long serviceId) {
        List<BarSlotDto> slots = serviceBookingService.getBarSlots(serviceId);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/{bookingId}/vnpay-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createVnpayUrl(
            @PathVariable Long bookingId,
            Authentication authentication,
            HttpServletRequest request) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        
        try {
            String paymentUrl = serviceBookingService.createVnpayPaymentUrl(bookingId, userId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tạo URL thanh toán thành công",
                    "paymentUrl", paymentUrl
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + ex.getMessage()
            ));
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<?> handleVnpayReturn(
            @RequestParam Map<String, String> allParams) {
        
        try {
            String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
            String vnp_TxnRef = allParams.get("vnp_TxnRef");
            
            if ("00".equals(vnp_ResponseCode)) {
                // Payment successful
                Long bookingId = Long.parseLong(vnp_TxnRef.split("_")[0]);
                String transactionRef = allParams.get("vnp_TransactionNo");
                OffsetDateTime paymentDate = OffsetDateTime.now();
                
                // Get user email from booking for email notification (since no authentication available)
                // We'll pass null for userEmail and let handleVnpayCallback get it from the booking
                serviceBookingService.handleVnpayCallback(bookingId, transactionRef, paymentDate, null);
                
                // Return HTML page with auto-redirect
                String deepLink = "qhomeapp://service-booking-result?bookingId=" + bookingId + "&status=success";
                String html = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                    "<meta http-equiv='refresh' content='2;url=%s'>" +
                    "<title>Thanh toán thành công</title></head><body>" +
                    "<h2>Thanh toán thành công!</h2>" +
                    "<p>Đang chuyển hướng...</p>" +
                    "<script>window.location.href='%s';</script>" +
                    "</body></html>",
                    deepLink, deepLink
                );
                
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .header("ngrok-skip-browser-warning", "true")
                        .body(html);
            } else {
                // Payment failed
                String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                        "<title>Thanh toán thất bại</title></head><body>" +
                        "<h2>Thanh toán thất bại</h2>" +
                        "<p>Vui lòng thử lại.</p>" +
                        "</body></html>";
                
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(html);
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Lỗi xử lý: " + ex.getMessage()
            ));
        }
    }
}

