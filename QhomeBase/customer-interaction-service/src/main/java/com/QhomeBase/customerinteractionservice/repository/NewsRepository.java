package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.News;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, Long> {
    @Query(value = """
    select n
    from content.news n
    where n.tenant_id = :tenantId and n.id = :id
""", nativeQuery = true)
    public News findByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);


    @Query(value = """
    select n
    from content.news n
    where n.tenant_id = :tenantId
""", nativeQuery = true)
    public List<News> findAll(@Param("tenantId") UUID tenantId);

    @Query(value = """
    select n
    from content.news n
    where n.id = :Id
""", nativeQuery = true)
    public News findById(@Param("Id") UUID id);






}
