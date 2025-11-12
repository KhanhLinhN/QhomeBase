package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    List<Invoice> findByPayerUnitId(UUID unitId);
    
    List<Invoice> findByPayerResidentId(UUID residentId);
    
    List<Invoice> findByStatus(InvoiceStatus status);
    
    List<Invoice> findByPayerResidentIdAndStatus(UUID residentId, InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.payerUnitId = :unitId AND i.cycleId = :cycleId")
    List<Invoice> findByPayerUnitIdAndCycleId(@Param("unitId") UUID unitId, @Param("cycleId") UUID cycleId);

    Optional<Invoice> findByVnpTransactionRef(String vnpTransactionRef);
}
