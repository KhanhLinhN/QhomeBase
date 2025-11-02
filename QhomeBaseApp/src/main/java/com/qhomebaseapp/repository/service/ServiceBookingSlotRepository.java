package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceBookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ServiceBookingSlotRepository extends JpaRepository<ServiceBookingSlot, Long> {
    
    List<ServiceBookingSlot> findByBooking_Id(Long bookingId);
    
    // Check for overlapping slots
    @Query("SELECT s FROM ServiceBookingSlot s WHERE s.service.id = :serviceId " +
           "AND s.slotDate = :date " +
           "AND (s.startTime < :endTime AND s.endTime > :startTime)")
    List<ServiceBookingSlot> findOverlappingSlots(
        @Param("serviceId") Long serviceId,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
}

