package com.qhomebaseapp.repository.news;

import com.qhomebaseapp.model.NewsTest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NewsTestRepository extends JpaRepository<NewsTest, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO qhomebaseapp.news_test
        (id, title, summary, cover_image_url, deep_link, created_at, raw_payload)
        VALUES (:id, :title, :summary, :coverImageUrl, :deepLink, :createdAt, CAST(:rawPayload AS jsonb))
        """, nativeQuery = true)
    void insertWithJsonb(
            @Param("id") UUID id,
            @Param("title") String title,
            @Param("summary") String summary,
            @Param("coverImageUrl") String coverImageUrl,
            @Param("deepLink") String deepLink,
            @Param("createdAt") Instant createdAt,
            @Param("rawPayload") String rawPayload
    );
}
