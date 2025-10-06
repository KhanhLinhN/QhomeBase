package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.CreateNewsRequest;
import com.qhomebaseapp.dto.news.NewsAttachmentDto;
import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.dto.registrationservice.UpdateNewsRequest;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
public class NewsMapper {

    public NewsDto toDto(News entity, boolean isRead) {
        if (entity == null) return null;

        return NewsDto.builder()
                .id(entity.getId())
                .categoryCode(entity.getCategory() != null ? entity.getCategory().getCode() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .content(entity.getContent())
                .author(entity.getAuthor())
                .source(entity.getSource())
                .publishedAt(entity.getPublishedAt())
                .pinned(entity.getPinned())
                .visibleToAll(entity.getVisibleToAll())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .read(isRead)
                .attachments(entity.getAttachments() != null
                        ? entity.getAttachments().stream()
                        .map(att -> NewsAttachmentDto.builder()
                                .id(att.getId())
                                .filename(att.getFilename())
                                .url(att.getUrl())
                                .build())
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    public News fromCreateRequest(CreateNewsRequest req, NewsCategory category, String username) {
        return News.builder()
                .category(category)
                .title(req.getTitle())
                .summary(req.getSummary())
                .content(req.getContent())
                .author(req.getAuthor())
                .source(req.getSource())
                .publishedAt(LocalDateTime.now())
                .pinned(req.getPinned())
                .visibleToAll(req.getVisibleToAll())
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void updateEntity(News entity, UpdateNewsRequest req, NewsCategory category, String username) {
        entity.setCategory(category);
        entity.setTitle(req.getTitle());
        entity.setSummary(req.getSummary());
        entity.setContent(req.getContent());
        entity.setAuthor(req.getAuthor());
        entity.setSource(req.getSource());
        entity.setPinned(req.getPinned());
        entity.setVisibleToAll(req.getVisibleToAll());
        entity.setUpdatedBy(username);
        entity.setUpdatedAt(LocalDateTime.now());
    }
}
