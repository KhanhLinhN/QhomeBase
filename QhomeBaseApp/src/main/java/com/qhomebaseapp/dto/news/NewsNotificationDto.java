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
public class NewsNotificationDto {
    private Long newsId;
    private String title;
    private String summary;
    private LocalDateTime publishedAt;
    private List<NewsAttachmentDto> attachments;
}
