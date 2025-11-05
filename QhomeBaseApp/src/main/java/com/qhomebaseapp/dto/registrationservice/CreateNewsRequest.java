package com.qhomebaseapp.dto.registrationservice;

// import com.qhomebaseapp.dto.news.NewsAttachmentDto; // Removed: News functionality removed
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
@Data @Builder
@NoArgsConstructor
@AllArgsConstructor


public class CreateNewsRequest {
    private UUID newsUuid;
    private String categoryCode;
    private String title;
    private String summary;
    private String bodyHtml;
    private String coverImageUrl;
    private String deepLink;
    private String status;
    private Instant publishAt;
    private Instant expireAt;
    private Instant receivedAt;
    private Boolean isRead;
    private String rawPayload;
    private Instant createdAt;
    private Instant updatedAt;

    // private List<NewsAttachmentDto> attachments; // Removed: News functionality removed
    private String vehicleType;
    private String licensePlate;
    private String vehicleBrand;
    private String vehicleColor;
    private String imageUrl;
}