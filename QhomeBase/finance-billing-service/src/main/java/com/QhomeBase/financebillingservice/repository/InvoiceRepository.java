package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.dto.BuildingDto;
import com.QhomeBase.financebillingservice.model.billingCycle;
import com.QhomeBase.financebillingservice.model.invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<invoice,UUID> {
    List<invoice> findByTenantId(UUID tenantId);
    //List<BuildingDto> findBuildingByTenantId (UUID tenantId);
    //List<invoice> findByTenantIdAndBuildingId(UUID tenantId, UUID customerId);
  /*  @Query("""
  select i from invoice i
  join i.unit u
  join u.building b
  where i.tenantId = :tenantId and b.id = :buildingId
  """) */
    Page<invoice> findByTenantId(UUID tenantId, Pageable pageable);

}
