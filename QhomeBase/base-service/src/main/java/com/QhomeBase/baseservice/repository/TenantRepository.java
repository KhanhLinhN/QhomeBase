package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Tenant;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    @Query("""
           select (count(t) > 0)
           from Tenant t
           where t.code = :code
           """)
    boolean existsByCode( @Param("code") String code);
    Optional<Tenant> findById(UUID id);

}
