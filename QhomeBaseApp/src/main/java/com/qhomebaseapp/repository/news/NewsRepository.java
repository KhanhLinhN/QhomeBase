package com.qhomebaseapp.repository.news;


import com.qhomebaseapp.model.News;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, Long> {
    Optional<News> findByNewsUuid(String newsUuid);
    Page<News> findAllByOrderByPublishAtDesc(Pageable pageable);

    @Query("""
                SELECT n FROM News n
                WHERE n.status = 'PUBLISHED'
                  AND n.publishAt <= CURRENT_TIMESTAMP
                  AND (n.expireAt IS NULL OR n.expireAt > CURRENT_TIMESTAMP)
                  AND NOT EXISTS (
                      SELECT 1 FROM NewsRead r
                      WHERE r.newsId = n.id AND r.userId = :userId
                  )
                ORDER BY n.publishAt DESC
            """)
    Page<News> findUnreadNewsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO qhomebaseapp.news
            (news_uuid, title, summary, cover_image_url, deep_link, received_at, raw_payload, created_at, updated_at)
            VALUES (:newsUuid, :title, :summary, :coverImageUrl, :deepLink, :receivedAt, CAST(:rawPayload AS jsonb), :createdAt, :updatedAt)
            ON CONFLICT (news_uuid) DO UPDATE
            SET title = EXCLUDED.title,
                summary = EXCLUDED.summary,
                cover_image_url = EXCLUDED.cover_image_url,
                deep_link = EXCLUDED.deep_link,
                raw_payload = EXCLUDED.raw_payload,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsertWithJsonb(
            @Param("newsUuid") UUID newsUuid,
            @Param("title") String title,
            @Param("summary") String summary,
            @Param("coverImageUrl") String coverImageUrl,
            @Param("deepLink") String deepLink,
            @Param("receivedAt") Instant receivedAt,
            @Param("rawPayload") String rawPayload,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt
    );
}