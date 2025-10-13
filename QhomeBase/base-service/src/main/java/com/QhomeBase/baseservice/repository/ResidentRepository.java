package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.ResidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResidentRepository extends JpaRepository<Resident, UUID> {
    
    List<Resident> findAllByTenantId(UUID tenantId);
    
    List<Resident> findAllByTenantIdAndStatus(UUID tenantId, ResidentStatus status);
    
    @Query("SELECT r FROM Resident r WHERE r.tenantId = :tenantId AND " +
           "(LOWER(r.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Resident> searchByTenantIdAndTerm(@Param("tenantId") UUID tenantId, @Param("searchTerm") String searchTerm);
    
    Optional<Resident> findByTenantIdAndPhone(UUID tenantId, String phone);
    
    Optional<Resident> findByTenantIdAndEmail(UUID tenantId, String email);
    
    Optional<Resident> findByTenantIdAndNationalId(UUID tenantId, String nationalId);
    
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);
    
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    
    boolean existsByTenantIdAndNationalId(UUID tenantId, String nationalId);
    
    boolean existsByTenantIdAndPhoneAndIdNot(UUID tenantId, String phone, UUID id);
    
    boolean existsByTenantIdAndEmailAndIdNot(UUID tenantId, String email, UUID id);
    
    boolean existsByTenantIdAndNationalIdAndIdNot(UUID tenantId, String nationalId, UUID id);
}

