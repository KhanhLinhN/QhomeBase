package com.qhomebaseapp.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.Instant;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class NewsDto {
    private Long id;
    private UUID newsUuid;
    private String categoryCode;
    private String categoryName;
    private String title;
    private String summary;
    private String bodyHtml;
    private String coverImageUrl;
    private String deepLink;
    private String status;
    private Instant publishAt;
    private Instant expireAt;
    private Instant receivedAt;
    private boolean isRead;
    private String rawPayload;
    private Instant createdAt;
    private Instant updatedAt;
    private List<NewsAttachmentDto> attachments;
}