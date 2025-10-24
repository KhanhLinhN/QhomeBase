package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.news.*;
import com.QhomeBase.customerinteractionservice.model.*;
import com.QhomeBase.customerinteractionservice.repository.NewsRepository;
import com.QhomeBase.customerinteractionservice.repository.NewsViewRepository;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsViewRepository newsViewRepository;
    private final NewsNotificationService notificationService;

    public NewsManagementResponse createNews(CreateNewsRequest request, UUID tenantId, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        News news = News.builder()
                .tenantId(tenantId)
                .title(request.getTitle())
                .summary(request.getSummary())
                .bodyHtml(request.getBodyHtml())
                .coverImageUrl(request.getCoverImageUrl())
                .status(request.getStatus())
                .publishAt(request.getPublishAt())
                .expireAt(request.getExpireAt())
                .displayOrder(request.getDisplayOrder())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (NewsImageDto imgDto : request.getImages()) {
                NewsImage image = NewsImage.builder()
                        .url(imgDto.getUrl())
                        .caption(imgDto.getCaption())
                        .sortOrder(imgDto.getSortOrder())
                        .fileSize(imgDto.getFileSize())
                        .contentType(imgDto.getContentType())
                        .build();
                news.addImage(image);
            }
        }

        if (request.getTargetType() == TargetType.ALL) {
            NewsTarget target = NewsTarget.builder()
                    .tenantId(tenantId)
                    .targetType(TargetType.ALL)
                    .build();
            news.addTarget(target);
        } else if (request.getBuildingIds() != null && !request.getBuildingIds().isEmpty()) {
            for (UUID buildingId : request.getBuildingIds()) {
                NewsTarget target = NewsTarget.builder()
                        .tenantId(tenantId)
                        .targetType(TargetType.BUILDING)
                        .buildingId(buildingId)
                        .build();
                news.addTarget(target);
            }
        }

        News savedNews = newsRepository.save(news);

        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.created(
            savedNews.getId(),
            savedNews.getTitle(),
            savedNews.getSummary(),
            savedNews.getCoverImageUrl()
        );
        notificationService.notifyNewsCreated(tenantId, wsMessage);

        return toManagementResponse(savedNews);
    }

    private NewsDetailResponse toDetailResponse(News news, Boolean isRead, java.time.Instant readAt) {
        return NewsDetailResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .bodyHtml(news.getBodyHtml())
                .coverImageUrl(news.getCoverImageUrl())
                .status(news.getStatus())
                .publishAt(news.getPublishAt())
                .expireAt(news.getExpireAt())
                .displayOrder(news.getDisplayOrder())
                .viewCount(news.getViewCount())
                .images(toImageDtos(news.getImages()))
                .targets(toTargetDtos(news.getTargets()))
                .isRead(isRead)
                .readAt(readAt)
                .createdBy(news.getCreatedBy())
                .createdAt(news.getCreatedAt())
                .updatedBy(news.getUpdatedBy())
                .updatedAt(news.getUpdatedAt())
                .build();
    }

    private List<NewsImageDto> toImageDtos(List<NewsImage> images) {
        if (images == null) return List.of();
        return images.stream()
                .map(img -> NewsImageDto.builder()
                        .id(img.getId())
                        .newsId(img.getNews().getId())
                        .url(img.getUrl())
                        .caption(img.getCaption())
                        .sortOrder(img.getSortOrder())
                        .fileSize(img.getFileSize())
                        .contentType(img.getContentType())
                        .build())
                .collect(Collectors.toList());
    }

    private List<NewsTargetDto> toTargetDtos(List<NewsTarget> targets) {
        if (targets == null) return List.of();
        return targets.stream()
                .map(t -> NewsTargetDto.builder()
                        .id(t.getId())
                        .targetType(t.getTargetType())
                        .buildingId(t.getBuildingId())
                        .build())
                .collect(Collectors.toList());
    }
    public NewsManagementResponse updateNews(UUID newsId, UUID tenantId, UpdateNewsRequest request, Authentication auth) {
        var principal = (UserPrincipal) auth.getPrincipal();
        UUID userId = principal.uid();
        
        News news = newsRepository.findByTenantIdAndId(tenantId, newsId);
        
        if (news == null) {
            throw new IllegalArgumentException("News not found with ID: " + newsId);
        }

        // Verify tenant ownership
        if (!news.getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        if (request.getTitle() != null) {
            news.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            news.setSummary(request.getSummary());
        }
        if (request.getBodyHtml() != null) {
            news.setBodyHtml(request.getBodyHtml());
        }
        if (request.getCoverImageUrl() != null) {
            news.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getStatus() != null) {
            news.setStatus(request.getStatus());
        }
        if (request.getPublishAt() != null) {
            news.setPublishAt(request.getPublishAt());
        }
        if (request.getExpireAt() != null) {
            news.setExpireAt(request.getExpireAt());
        }
        if (request.getDisplayOrder() != null) {
            news.setDisplayOrder(request.getDisplayOrder());
        }
        news.setUpdatedBy(userId);

        News updated = newsRepository.save(news);
        
        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.updated(
            updated.getId(),
            updated.getTitle(),
            updated.getSummary(),
            updated.getCoverImageUrl()
        );
        notificationService.notifyNewsUpdated(tenantId, wsMessage);
        
        return toManagementResponse(updated);
    }
    
    public NewsManagementResponse deleteNews(UUID newsId, UUID tenantId, UUID userId) {
        News news = newsRepository.findByTenantIdAndId(tenantId, newsId);
        
        if (news == null) {
            throw new IllegalArgumentException("News not found with ID: " + newsId);
        }

        if (!news.getTenantId().equals(tenantId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }

        news.setStatus(NewsStatus.ARCHIVED);
        news.setUpdatedBy(userId);
        
        News deleted = newsRepository.save(news);
        
        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.deleted(deleted.getId());
        notificationService.notifyNewsDeleted(tenantId, wsMessage);
        
        return toManagementResponse(deleted);
    }

    public List<NewsManagementResponse> getNewsInTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required");
        }

        return newsRepository.findAll(tenantId)
                .stream()
                .map(this::toManagementResponse)
                .collect(Collectors.toList());
    }

    public NewsManagementResponse getNewsDetail(UUID newsId, UUID tenantId) {
        News news = newsRepository.findByTenantIdAndId(tenantId, newsId);
        
        if (news == null) {
            throw new IllegalArgumentException("News not found with ID: " + newsId);
        }

        return toManagementResponse(news);
    }

    public NewsDetailResponse getNewsForResident(UUID newsId, UUID tenantId, UUID residentId) {
        News news = newsRepository.findByTenantIdAndId(tenantId, newsId);
        
        if (news == null) {
            throw new IllegalArgumentException("News not found with ID: " + newsId);
        }

        Optional<NewsView> view = newsViewRepository.findByNewsIdAndResidentId(newsId, residentId);
        Boolean isRead = view.isPresent();
        Instant readAt = view.map(NewsView::getViewedAt).orElse(null);

        return toDetailResponse(news, isRead, readAt);
    }

    public List<NewsDetailResponse> getNewsForResident(UUID tenantId, UUID residentId) {
        List<News> newsList = newsRepository.findAll(tenantId);

        Map<UUID, NewsView> viewMap = newsViewRepository.findByResidentId(residentId, tenantId)
                .stream()
                .collect(Collectors.toMap(v -> v.getNews().getId(), v -> v));

        return newsList.stream()
                .map(news -> {
                    NewsView view = viewMap.get(news.getId());
                    Boolean isRead = (view != null);
                    Instant readAt = (view != null) ? view.getViewedAt() : null;
                    return toDetailResponse(news, isRead, readAt);
                })
                .collect(Collectors.toList());
    }

    public MarkAsReadResponse markAsRead(UUID newsId, UUID tenantId, UUID residentId) {
        News news = newsRepository.findByTenantIdAndId(tenantId, newsId);
        
        if (news == null) {
            throw new IllegalArgumentException("News not found with ID: " + newsId);
        }

        Optional<NewsView> existingView = newsViewRepository.findByNewsIdAndResidentId(newsId, residentId);
        
        if (existingView.isPresent()) {
            return MarkAsReadResponse.alreadyRead(existingView.get().getViewedAt());
        }

        NewsView newsView = NewsView.forResident(news, tenantId, residentId);
        newsViewRepository.save(newsView);

        news.incrementViewCount();
        newsRepository.save(news);

        return MarkAsReadResponse.success(newsView.getViewedAt());
    }

    public UnreadCountResponse countUnreadNews(UUID tenantId, UUID residentId) {
        Long count = newsViewRepository.countUnreadByResident(tenantId, residentId);
        return UnreadCountResponse.of(count);
    }

    private NewsManagementResponse toManagementResponse(News news) {
        return NewsManagementResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .bodyHtml(news.getBodyHtml())
                .coverImageUrl(news.getCoverImageUrl())
                .status(news.getStatus())
                .publishAt(news.getPublishAt())
                .expireAt(news.getExpireAt())
                .displayOrder(news.getDisplayOrder())
                .viewCount(news.getViewCount())
                .images(toImageDtos(news.getImages()))
                .targets(toTargetDtos(news.getTargets()))
                .createdBy(news.getCreatedBy())
                .createdAt(news.getCreatedAt())
                .updatedBy(news.getUpdatedBy())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}