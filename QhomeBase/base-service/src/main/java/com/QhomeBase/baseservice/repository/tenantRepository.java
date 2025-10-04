package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.tenant;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface tenantRepository extends JpaRepository<tenant, UUID> {
    @Query("""
           select (count(t) > 0)
           from tenant t
           where t.code = :code
           """)
    boolean existsByCode( @Param("code") String code);
    Optional<tenant> findById(UUID id);

}
