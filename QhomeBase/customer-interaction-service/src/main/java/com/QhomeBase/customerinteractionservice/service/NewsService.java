package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.config.AppConfig;
import com.QhomeBase.customerinteractionservice.dto.news.*;
import com.QhomeBase.customerinteractionservice.model.*;
import com.QhomeBase.customerinteractionservice.repository.NewsRepository;
import com.QhomeBase.customerinteractionservice.repository.NewsViewRepository;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
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
    private final AppConfig appConfig;

    public NewsManagementResponse createNews(CreateNewsRequest request, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();

        News news = News.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .bodyHtml(request.getBodyHtml())
                // normalize coverImageUrl BEFORE saving
                .coverImageUrl(normalizeFileUrl(request.getCoverImageUrl()))
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
                        .url(normalizeFileUrl(imgDto.getUrl()))
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
                    .targetType(TargetType.ALL)
                    .build();
            news.addTarget(target);
        } else if (request.getBuildingIds() != null && !request.getBuildingIds().isEmpty()) {
            for (UUID buildingId : request.getBuildingIds()) {
                NewsTarget target = NewsTarget.builder()
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
                normalizeFileUrl(savedNews.getCoverImageUrl()),
                savedNews.getStatus().name()
        );
        notificationService.notifyNewsCreated(wsMessage);

        return toManagementResponse(savedNews);
    }

    private String normalizeFileUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String base = appConfig.getFileBaseUrl();

        try {
            if (!url.startsWith("http")) {
                if (url.startsWith("/")) {
                    return base.endsWith("/") ? base.substring(0, base.length()-1) + url : base + url;
                } else {
                    return base.endsWith("/") ? base + url : base + "/" + url;
                }
            }

            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return url;

            if ("localhost".equalsIgnoreCase(host) || "192.168.100.33".equals(host)) {
                URI baseUri = new URI(base);
                String scheme = baseUri.getScheme() != null ? baseUri.getScheme() : uri.getScheme();
                String newHost = baseUri.getHost();
                int newPort = baseUri.getPort();
                String path = uri.getRawPath();
                String query = uri.getRawQuery();
                String fragment = uri.getRawFragment();
                URI replaced = new URI(scheme, null, newHost, newPort, path, query, fragment);
                return replaced.toString();
            }

            return url;
        } catch (URISyntaxException e) {
            if (url.contains("localhost")) {
                String candidate = url.replace("localhost", base.replace("http://", "").replace("https://", ""));
                return candidate;
            }
            return url;
        }
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
    public NewsManagementResponse updateNews(UUID newsId, UpdateNewsRequest request, Authentication auth) {
        var principal = (UserPrincipal) auth.getPrincipal();
        UUID userId = principal.uid();
        
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));
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
        notificationService.notifyNewsUpdated(wsMessage);
        
        return toManagementResponse(updated);
    }
    
    public NewsManagementResponse deleteNews(UUID newsId, UUID userId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        news.setStatus(NewsStatus.ARCHIVED);
        news.setUpdatedBy(userId);
        
        News deleted = newsRepository.save(news);
        
        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.deleted(deleted.getId());
        notificationService.notifyNewsDeleted(wsMessage);
        
        return toManagementResponse(deleted);
    }

    public List<NewsManagementResponse> getAllNews() {
        return newsRepository.findAll()
                .stream()
                .map(this::toManagementResponse)
                .collect(Collectors.toList());
    }

    public NewsManagementResponse getNewsDetail(UUID newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        return toManagementResponse(news);
    }

    public NewsDetailResponse getNewsForResident(UUID newsId, UUID residentId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        Optional<NewsView> view = newsViewRepository.findByNewsIdAndResidentId(newsId, residentId);
        Boolean isRead = view.isPresent();
        Instant readAt = view.map(NewsView::getViewedAt).orElse(null);

        return toDetailResponse(news, isRead, readAt);
    }

    public List<NewsDetailResponse> getNewsForResident(UUID residentId) {
        List<News> newsList = newsRepository.findAll();

        Map<UUID, NewsView> viewMap = newsViewRepository.findByResidentId(residentId)
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

    public MarkAsReadResponse markAsRead(UUID newsId, UUID residentId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        Optional<NewsView> existingView = newsViewRepository.findByNewsIdAndResidentId(newsId, residentId);
        
        if (existingView.isPresent()) {
            return MarkAsReadResponse.alreadyRead(existingView.get().getViewedAt());
        }

        NewsView newsView = NewsView.forResident(news, residentId);
        newsViewRepository.save(newsView);

        news.incrementViewCount();
        newsRepository.save(news);

        return MarkAsReadResponse.success(newsView.getViewedAt());
    }

    public UnreadCountResponse countUnreadNews(UUID residentId) {
        Long count = newsViewRepository.countUnreadByResident(residentId);
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