package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {
    
    List<ServiceBooking> findByUser_Id(Long userId);
    
    List<ServiceBooking> findByService_Id(Long serviceId);
    
    // Check for overlapping bookings on the same date and time
    @Query("SELECT b FROM ServiceBooking b WHERE b.service.id = :serviceId " +
           "AND b.bookingDate = :date " +
           "AND b.status IN ('PENDING', 'APPROVED', 'PAID') " +
           "AND (b.startTime < :endTime AND b.endTime > :startTime)")
    List<ServiceBooking> findOverlappingBookings(
        @Param("serviceId") Long serviceId,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    // Find all bookings for a service on a specific date
    @Query("SELECT b FROM ServiceBooking b WHERE b.service.id = :serviceId " +
           "AND b.bookingDate = :date " +
           "AND b.status IN ('PENDING', 'APPROVED', 'PAID')")
    List<ServiceBooking> findBookingsByServiceAndDate(
        @Param("serviceId") Long serviceId,
        @Param("date") LocalDate date
    );
    
    // Find unpaid bookings by user
    List<ServiceBooking> findByUser_IdAndPaymentStatus(Long userId, String paymentStatus);
    
    // Find unpaid bookings created more than 10 minutes ago
    @Query("SELECT b FROM ServiceBooking b WHERE b.paymentStatus = 'UNPAID' " +
           "AND b.createdAt < :cutoffTime " +
           "AND b.status = 'PENDING'")
    List<ServiceBooking> findUnpaidBookingsOlderThan(
        @Param("cutoffTime") OffsetDateTime cutoffTime
    );
}

