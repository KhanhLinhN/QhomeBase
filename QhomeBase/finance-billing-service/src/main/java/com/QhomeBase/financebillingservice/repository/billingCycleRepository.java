package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.billingCycle;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface billingCycleRepository extends JpaRepository<billingCycle, UUID> {
    public List<billingCycle> findByTenantId(UUID tenantId);
    /*@Query("select b from billingCycle b " +
            "where b.tenantId = :tenantId " +
    "and year(b.periodTo) = :year "+ "order by b.periodTo desc ") */
    @Query("""
    select b 
    from billingCycle b 
    where b.tenantId = :tenantId 
      and year(b.periodTo)= :year 
    order by b.periodTo desc
    """)
    public List<billingCycle> loadPeriod(@Param("tenantId") UUID tenantId,@Param("year") int year);
    @Query(""" 
select b from billingCycle b
where b.tenantId = :tenantId
and b.periodFrom = :periodFrom
and b.periodTo = :periodTo
""")
    public List<billingCycle> findListByTime(@Param("tenantId") UUID tenantId, @Param("periodFrom")LocalDate periodFrom, @Param("periodTo") LocalDate periodTo);
}
