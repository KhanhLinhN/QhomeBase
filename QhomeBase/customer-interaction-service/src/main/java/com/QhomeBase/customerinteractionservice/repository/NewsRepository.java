package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.News;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, UUID> {
    @Query(value = """
    SELECT * FROM content.news n
    WHERE n.tenant_id = :tenantId AND n.id = :id
""", nativeQuery = true)
    public News findByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);


    @Query(value = """
    SELECT * FROM content.news n
    WHERE n.tenant_id = :tenantId
""", nativeQuery = true)
    public List<News> findAll(@Param("tenantId") UUID tenantId);






}
