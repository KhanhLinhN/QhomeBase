package com.QhomeBase.customerinteractionservice.dto.news;

import com.QhomeBase.customerinteractionservice.model.NewsStatus;
import com.QhomeBase.customerinteractionservice.model.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNewsRequest {

    private String title;
    private String summary;
    private String bodyHtml;
    private String coverImageUrl;
    private NewsStatus status;
    private Instant publishAt;
    private Instant expireAt;
    private Integer displayOrder;
    private TargetType targetType;
    private List<UUID> buildingIds;
    private List<NewsImageDto> images;
}

