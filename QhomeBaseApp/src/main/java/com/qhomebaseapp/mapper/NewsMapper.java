package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.model.News;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {

    public NewsDto toDto(News entity) {
        if (entity == null) return null;

        return NewsDto.builder()
                .id(entity.getId())
                .newsUuid(entity.getNewsUuid())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .coverImageUrl(entity.getCoverImageUrl())
                .deepLink(entity.getDeepLink())
                .status(entity.getStatus())
                .publishAt(entity.getPublishAt())
                .receivedAt(entity.getReceivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}