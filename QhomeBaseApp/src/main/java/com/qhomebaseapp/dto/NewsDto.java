package com.qhomebaseapp.dto;

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

    // category info
    private String categoryCode;
    private String categoryName;

    // content
    private String title;
    private String summary;
    private String content;

    // meta info
    private String author;
    private String source;

    private LocalDateTime publishedAt;
    private Boolean pinned;
    private Boolean visibleToAll;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // user-specific info
    private boolean read;

    private List<NewsAttachmentDto> attachments;
}
