package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBooking;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, UUID> {

    List<ServiceBooking> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ServiceBooking> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);

    List<ServiceBooking> findAllByStatusOrderByCreatedAtDesc(ServiceBookingStatus status);

    List<ServiceBooking> findAllByServiceIdAndStatusOrderByCreatedAtDesc(UUID serviceId, ServiceBookingStatus status);

    Optional<ServiceBooking> findByIdAndUserId(UUID id, UUID userId);

    List<ServiceBooking> findAllByBookingDateBetweenOrderByCreatedAtDesc(LocalDate start, LocalDate end);

    List<ServiceBooking> findAllByServiceIdAndBookingDateBetweenOrderByCreatedAtDesc(UUID serviceId, LocalDate start, LocalDate end);
}






