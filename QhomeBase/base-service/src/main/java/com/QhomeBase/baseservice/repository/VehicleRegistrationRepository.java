package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.VehicleRegistrationRequest;
import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VehicleRegistrationRepository extends JpaRepository<VehicleRegistrationRequest, UUID> {
    
    List<VehicleRegistrationRequest> findAllByTenantId(UUID tenantId);
    
    List<VehicleRegistrationRequest> findAllByTenantIdAndStatus(UUID tenantId, VehicleRegistrationStatus status);
    
    List<VehicleRegistrationRequest> findAllByRequestedBy(UUID requestedBy);
    
    List<VehicleRegistrationRequest> findAllByApprovedBy(UUID approvedBy);
    
    List<VehicleRegistrationRequest> findAllByVehicleId(UUID vehicleId);
    
    @Query("SELECT v FROM VehicleRegistrationRequest v WHERE v.tenantId = :tenantId AND " +
           "(LOWER(v.reason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.note) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<VehicleRegistrationRequest> searchByTenantIdAndTerm(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);
    
    @Query("SELECT v FROM VehicleRegistrationRequest v JOIN FETCH v.vehicle WHERE v.tenantId = :tenantId")
    List<VehicleRegistrationRequest> findAllByTenantIdWithVehicle(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT v FROM VehicleRegistrationRequest v JOIN FETCH v.vehicle WHERE v.id = :id")
    VehicleRegistrationRequest findByIdWithVehicle(@Param("id") UUID id);
    
    List<VehicleRegistrationRequest> findAllByStatus(VehicleRegistrationStatus status);
    
    boolean existsByTenantIdAndVehicleId(UUID tenantId, UUID vehicleId);
    
    boolean existsByTenantIdAndVehicleIdAndIdNot(UUID tenantId, UUID vehicleId, UUID id);
}


