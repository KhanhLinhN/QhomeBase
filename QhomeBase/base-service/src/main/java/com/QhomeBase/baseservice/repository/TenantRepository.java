package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    @Query("""
           select (count(t) > 0)
           from Tenant t
           where t.code = :code
           """)
    boolean existsByCode(@Param("code") String code);
}
