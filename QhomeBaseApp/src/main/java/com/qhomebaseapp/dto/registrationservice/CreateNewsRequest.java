package com.qhomebaseapp.dto.registrationservice;

import com.qhomebaseapp.dto.news.NewsAttachmentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewsRequest {
    private String categoryCode;
    private String title;
    private String summary;
    private String content;
    private String author;
    private String source;
    private Boolean pinned;
    private Boolean visibleToAll;
    private LocalDateTime publishedAt;
    private List<NewsAttachmentDto> attachments;
    private String vehicleType;
    private String licensePlate;
    private String vehicleBrand;
    private String vehicleColor;
    private String imageUrl;

}