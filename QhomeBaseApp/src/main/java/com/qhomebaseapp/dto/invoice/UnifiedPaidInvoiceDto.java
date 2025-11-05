package com.qhomebaseapp.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Unified DTO for all types of paid invoices
 * Used for displaying paid invoices grouped by category and month
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedPaidInvoiceDto {
    private String id; // Invoice ID, Booking ID, or Registration ID
    private String category; // "ELECTRICITY", "SERVICE_BOOKING", "VEHICLE_REGISTRATION"
    private String categoryName; // Display name: "Hóa đơn điện", "Hóa đơn dịch vụ", "Hóa đơn đăng ký thẻ xe"
    private String title; // Invoice code, Service name, or Vehicle registration info
    private String description; // Additional details
    private BigDecimal amount; // Total amount paid
    private OffsetDateTime paymentDate; // When payment was made
    private String paymentGateway; // VNPAY, etc.
    private String status; // Original status
    private String reference; // Transaction reference or invoice code
    
    // Additional fields for different invoice types
    private String invoiceCode; // For electricity invoices
    private String serviceName; // For service bookings
    private String licensePlate; // For vehicle registrations
    private String vehicleType; // For vehicle registrations
}

