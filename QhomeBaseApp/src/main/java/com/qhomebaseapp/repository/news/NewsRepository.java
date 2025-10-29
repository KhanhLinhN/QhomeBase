package com.qhomebaseapp.repository.news;


import com.qhomebaseapp.dto.news.NewsDto;
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
        SELECT new com.qhomebaseapp.dto.news.NewsDto(
            n.id,
            n.newsUuid,
            n.title,
            n.summary,
            n.coverImageUrl,
            n.deepLink,
            n.status,
            n.publishAt,
            n.receivedAt,
            n.createdAt,
            n.updatedAt,
            CASE WHEN nr.isRead = true THEN true ELSE false END
        )
        FROM News n
        LEFT JOIN NewsRead nr
          ON nr.newsId = n.id
         AND nr.userId = :userId
        ORDER BY n.publishAt DESC
    """)
    Page<NewsDto> findAllWithUserReadStatus(@Param("userId") Long userId, Pageable pageable);

    @Query("""
                SELECT n FROM News n
                WHERE n.publishAt <= CURRENT_TIMESTAMP
                  AND NOT EXISTS (
                      SELECT 1 FROM NewsRead r
                      WHERE r.newsId = n.id AND r.userId = :userId
                  )
                ORDER BY n.publishAt DESC
            """)
    Page<News> findUnreadNewsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );


    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO qhomebaseapp.news
            (news_uuid, title, summary, cover_image_url, deep_link, received_at, raw_payload, created_at, updated_at, status, publish_at)
            VALUES (:newsUuid, :title, :summary, :coverImageUrl, :deepLink, :receivedAt, CAST(:rawPayload AS jsonb), :createdAt, :updatedAt, :status, :publishAt)
            ON CONFLICT (news_uuid) DO UPDATE
            SET title = EXCLUDED.title,
                summary = EXCLUDED.summary,
                cover_image_url = EXCLUDED.cover_image_url,
                deep_link = EXCLUDED.deep_link,
                raw_payload = EXCLUDED.raw_payload,
                updated_at = EXCLUDED.updated_at,
                status = EXCLUDED.status,
                publish_at = EXCLUDED.publish_at
            """, nativeQuery = true)
    void upsertWithJsonb(
            @Param("newsUuid") UUID newsUuid,
            @Param("title") String title,
            @Param("summary") String summary,
            @Param("coverImageUrl") String coverImageUrl,
            @Param("deepLink") String deepLink,
            @Param("receivedAt") Instant receivedAt,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt,
            @Param("status") String status,
            @Param("publishAt") Instant publishAt,
            @Param("rawPayload") String rawPayload
    );
}