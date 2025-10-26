package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.CreateNewsRequest;
import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.dto.registrationservice.UpdateNewsRequest;
import com.qhomebaseapp.model.News;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NewsMapper {

    public NewsDto toDto(News entity) {
        if (entity == null) return null;

        return NewsDto.builder()
                .id(entity.getId())
                .newsUuid(entity.getNewsUuid())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .bodyHtml(entity.getBodyHtml())
                .coverImageUrl(entity.getCoverImageUrl())
                .deepLink(entity.getDeepLink())
                .status(entity.getStatus())
                .publishAt(entity.getPublishAt())
                .expireAt(entity.getExpireAt())
                .receivedAt(entity.getReceivedAt())
                .isRead(entity.getIsRead())
                .rawPayload(entity.getRawPayload())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public News fromCreateRequest(CreateNewsRequest req) {
        return News.builder()
                .newsUuid(req.getNewsUuid())
                .title(req.getTitle())
                .summary(req.getSummary())
                .bodyHtml(req.getBodyHtml())
                .coverImageUrl(req.getCoverImageUrl())
                .deepLink(req.getDeepLink())
                .status(req.getStatus())
                .publishAt(req.getPublishAt())
                .expireAt(req.getExpireAt())
                .receivedAt(Instant.now())
                .isRead(false)
                .rawPayload(req.getRawPayload())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void updateEntity(News entity, UpdateNewsRequest req) {
        entity.setTitle(req.getTitle());
        entity.setSummary(req.getSummary());
        entity.setBodyHtml(req.getBodyHtml());
        entity.setCoverImageUrl(req.getCoverImageUrl());
        entity.setDeepLink(req.getDeepLink());
        entity.setStatus(req.getStatus());
        entity.setPublishAt(req.getPublishAt());
        entity.setExpireAt(req.getExpireAt());
        entity.setRawPayload(req.getRawPayload());
        entity.setUpdatedAt(Instant.now());
    }
}