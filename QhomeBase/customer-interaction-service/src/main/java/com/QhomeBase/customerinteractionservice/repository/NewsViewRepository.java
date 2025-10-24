package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.NewsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsViewRepository extends JpaRepository<NewsView, UUID> {


    @Query("SELECT nv FROM NewsView nv WHERE nv.news.id = :newsId AND nv.residentId = :residentId")
    Optional<NewsView> findByNewsIdAndResidentId(@Param("newsId") UUID newsId, @Param("residentId") UUID residentId);


    @Query("SELECT nv FROM NewsView nv WHERE nv.news.id = :newsId AND nv.userId = :userId")
    Optional<NewsView> findByNewsIdAndUserId(@Param("newsId") UUID newsId, @Param("userId") UUID userId);


    @Query("SELECT nv FROM NewsView nv WHERE nv.residentId = :residentId AND nv.tenantId = :tenantId")
    List<NewsView> findByResidentId(@Param("residentId") UUID residentId, @Param("tenantId") UUID tenantId);


    @Query("SELECT nv FROM NewsView nv WHERE nv.userId = :userId AND nv.tenantId = :tenantId")
    List<NewsView> findByUserId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);


    @Query("""
        SELECT COUNT(n) FROM News n
        WHERE n.tenantId = :tenantId
        AND n.status = 'PUBLISHED'
        AND NOT EXISTS (
            SELECT 1 FROM NewsView nv
            WHERE nv.news.id = n.id
            AND nv.residentId = :residentId
        )
    """)
    Long countUnreadByResident(@Param("tenantId") UUID tenantId, @Param("residentId") UUID residentId);


    @Query("""
        SELECT COUNT(n) FROM News n
        WHERE n.tenantId = :tenantId
        AND n.status = 'PUBLISHED'
        AND NOT EXISTS (
            SELECT 1 FROM NewsView nv
            WHERE nv.news.id = n.id
            AND nv.userId = :userId
        )
    """)
    Long countUnreadByUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}




