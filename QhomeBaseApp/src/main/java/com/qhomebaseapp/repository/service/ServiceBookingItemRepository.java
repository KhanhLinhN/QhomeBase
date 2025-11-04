package com.qhomebaseapp.repository.service;

import com.qhomebaseapp.model.ServiceBookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceBookingItemRepository extends JpaRepository<ServiceBookingItem, Long> {
    
    List<ServiceBookingItem> findByBooking_Id(Long bookingId);
    
    List<ServiceBookingItem> findByBooking_IdAndItemType(Long bookingId, String itemType);
}

