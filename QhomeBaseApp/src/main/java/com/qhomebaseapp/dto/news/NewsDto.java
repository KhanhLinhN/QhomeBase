package com.qhomebaseapp.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsDto {
    private Long id;
    private String categoryCode;
    private String categoryName;
    private String title;
    private String summary;
    private String content;
    private String author;
    private String source;
    private LocalDateTime publishedAt;
    private Boolean pinned;
    private Boolean visibleToAll;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private boolean isRead;
    private List<NewsAttachmentDto> attachments;
}
