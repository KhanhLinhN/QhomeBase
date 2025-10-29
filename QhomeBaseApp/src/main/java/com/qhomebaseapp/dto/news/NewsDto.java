package com.qhomebaseapp.dto.news;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Data
@NoArgsConstructor
@Builder
public class NewsDto {
    private Long id;
    private String newsUuid;
    private String title;
    private String summary;
    private String coverImageUrl;
    private String deepLink;
    private String status;
    private Instant publishAt;
    private Instant receivedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isRead;

    public NewsDto(
            Long id,
            String newsUuid,
            String title,
            String summary,
            String coverImageUrl,
            String deepLink,
            String status,
            Instant publishAt,
            Instant receivedAt,
            Instant createdAt,
            Instant updatedAt,
            boolean isRead
    ) {
        this.id = id;
        this.newsUuid = newsUuid;
        this.title = title;
        this.summary = summary;
        this.coverImageUrl = coverImageUrl;
        this.deepLink = deepLink;
        this.status = status;
        this.publishAt = publishAt;
        this.receivedAt = receivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isRead = isRead;
    }
}