package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.BillingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycle, UUID> {

    List<BillingCycle> findByTenantId(UUID tenantId);

    @Query("""
        select b 
        from BillingCycle b 
        where b.tenantId = :tenantId 
          and year(b.periodTo) = :year 
        order by b.periodTo desc
        """)
    List<BillingCycle> loadPeriod(@Param("tenantId") UUID tenantId, @Param("year") int year);

    @Query("""
        select b 
        from BillingCycle b
        where b.tenantId = :tenantId
          and b.periodFrom = :periodFrom
          and b.periodTo = :periodTo
        """)
    List<BillingCycle> findListByTime(@Param("tenantId") UUID tenantId,
                                      @Param("periodFrom") LocalDate periodFrom,
                                      @Param("periodTo") LocalDate periodTo);
}