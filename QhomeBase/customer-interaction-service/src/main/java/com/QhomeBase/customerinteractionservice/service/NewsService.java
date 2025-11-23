package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdDto;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdMemberDto;
import com.QhomeBase.customerinteractionservice.client.dto.UnitDto;
import com.QhomeBase.customerinteractionservice.dto.news.*;
import com.QhomeBase.customerinteractionservice.model.*;
import com.QhomeBase.customerinteractionservice.repository.NewsRepository;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsNotificationService notificationService;
    private final NotificationPushService notificationPushService;
    private final BaseServiceClient baseServiceClient;

    public NewsManagementResponse createNews(CreateNewsRequest request, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();

        validateNewsScope(request.getScope(), request.getTargetRole(), request.getTargetBuildingId());

        News news = News.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .bodyHtml(request.getBodyHtml())
                .coverImageUrl(request.getCoverImageUrl())
                .status(request.getStatus())
                .publishAt(request.getPublishAt())
                .expireAt(request.getExpireAt())
                .displayOrder(request.getDisplayOrder())
                .scope(request.getScope())
                .targetRole(request.getTargetRole())
                .targetBuildingId(request.getTargetBuildingId())
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

        News savedNews = newsRepository.save(news);

        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.created(
                savedNews.getId(),
                savedNews.getTitle(),
                savedNews.getSummary(),
                savedNews.getCoverImageUrl());
        notificationService.notifyNewsCreated(wsMessage);
        if (savedNews.isActive()) {
            notificationPushService.sendNewsCreatedPush(savedNews);
        }

        return toManagementResponse(savedNews);
    }

    private NewsDetailResponse toDetailResponse(News news) {
        // Normalize coverImageUrl: convert empty string to null
        String coverImageUrl = news.getCoverImageUrl();
        if (coverImageUrl != null && coverImageUrl.trim().isEmpty()) {
            coverImageUrl = null;
        }
        
        return NewsDetailResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .bodyHtml(news.getBodyHtml())
                .coverImageUrl(coverImageUrl)
                .status(news.getStatus())
                .publishAt(news.getPublishAt())
                .expireAt(news.getExpireAt())
                .displayOrder(news.getDisplayOrder())
                .viewCount(news.getViewCount())
                .images(toImageDtos(news.getImages()))
                .createdBy(news.getCreatedBy())
                .createdAt(news.getCreatedAt())
                .updatedBy(news.getUpdatedBy())
                .updatedAt(news.getUpdatedAt())
                .build();
    }

    private List<NewsImageDto> toImageDtos(List<NewsImage> images) {
        if (images == null)
            return List.of();
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
        if (request.getScope() != null) {
            news.setScope(request.getScope());
            validateNewsScope(request.getScope(), request.getTargetRole(), request.getTargetBuildingId());

            if (request.getScope() == NotificationScope.INTERNAL) {
                news.setTargetRole(request.getTargetRole());
                news.setTargetBuildingId(null);
            } else if (request.getScope() == NotificationScope.EXTERNAL) {
                news.setTargetRole(null);
                news.setTargetBuildingId(request.getTargetBuildingId());
            }
        } else if (news.getScope() != null) {
            NotificationScope currentScope = news.getScope();
            validateNewsScope(currentScope, request.getTargetRole(), request.getTargetBuildingId());

            if (currentScope == NotificationScope.INTERNAL && request.getTargetRole() != null) {
                news.setTargetRole(request.getTargetRole());
            } else if (currentScope == NotificationScope.EXTERNAL && request.getTargetBuildingId() != null) {
                news.setTargetBuildingId(request.getTargetBuildingId());
            }
        }
        news.setUpdatedBy(userId);

        News updated = newsRepository.save(news);

        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.updated(
                updated.getId(),
                updated.getTitle(),
                updated.getSummary(),
                updated.getCoverImageUrl());
        notificationService.notifyNewsUpdated(wsMessage);
        if (updated.isActive()) {
            notificationPushService.sendNewsUpdatedPush(updated);
        }

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

        if (!shouldShowNewsToResident(news, residentId)) {
            throw new IllegalArgumentException("News not accessible for this resident");
        }

        return toDetailResponse(news);
    }

    public NewsPagedResponse getNewsForResidentPaged(UUID residentId, int page, int size) {
        List<News> allNews = newsRepository.findAll();

        List<NewsDetailResponse> filteredAndSorted = allNews.stream()
                // Only show news with status = PUBLISHED (exclude DRAFT, SCHEDULED, HIDDEN, EXPIRED, ARCHIVED)
                .filter(news -> news.getStatus() == NewsStatus.PUBLISHED)
                .filter(news -> shouldShowNewsToResident(news, residentId))
                .sorted((n1, n2) -> {
                    // Sort by publishAt DESC (newest first, from largest to smallest date)
                    // News with newest publishAt will be on page 1 (first page)
                    // If publishAt is null, fallback to createdAt
                    Instant publishAt1 = n1.getPublishAt() != null ? n1.getPublishAt() : n1.getCreatedAt();
                    Instant publishAt2 = n2.getPublishAt() != null ? n2.getPublishAt() : n2.getCreatedAt();
                    
                    if (publishAt1 != null && publishAt2 != null) {
                        // Sort DESC: publishAt2.compareTo(publishAt1) means newer date comes first
                        return publishAt2.compareTo(publishAt1);
                    }
                    // If one publishAt is null, prioritize the one with publishAt
                    if (publishAt1 != null) return -1;
                    if (publishAt2 != null) return 1;
                    return 0;
                })
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        // Calculate pagination
        long totalElements = filteredAndSorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Ensure page is within valid range
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filteredAndSorted.size());
        List<NewsDetailResponse> pagedContent = start < filteredAndSorted.size() 
                ? filteredAndSorted.subList(start, end)
                : new ArrayList<>();

        return NewsPagedResponse.builder()
                .content(pagedContent)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .isFirst(page == 0)
                .isLast(page >= totalPages - 1 || totalPages == 0)
                .build();
    }

    // Backward compatibility method - returns first page
    public List<NewsDetailResponse> getNewsForResident(UUID residentId) {
        NewsPagedResponse pagedResponse = getNewsForResidentPaged(residentId, 0, 7);
        return pagedResponse.getContent();
    }

    private boolean shouldShowNewsToResident(News news, UUID residentId) {
        // Note: Status filter (PUBLISHED only) is already applied before calling this method
        // So we don't need to check isActive() here, but we still check publishAt/expireAt dates
        Instant now = Instant.now();
        if (news.getPublishAt() != null && news.getPublishAt().isAfter(now)) {
            return false; // Not published yet
        }
        if (news.getExpireAt() != null && news.getExpireAt().isBefore(now)) {
            return false; // Already expired
        }

        NotificationScope scope = news.getScope();
        if (scope == null) {
            return true;
        }

        if (scope == NotificationScope.INTERNAL) {
            return false;
        }
        if (scope == NotificationScope.EXTERNAL) {
            if (news.getTargetBuildingId() == null) {
                return true;
            }

            UUID residentBuildingId = getResidentBuildingId(residentId);
            if (residentBuildingId == null) {
                log.debug("Unable to resolve building for resident {} -> allow news {}", residentId, news.getId());
                return true;
            }
            return residentBuildingId.equals(news.getTargetBuildingId());
        }
        if (news.getScope() == null) {
            return true;
        }

        if (news.getScope() == NotificationScope.INTERNAL) {
            return false;
        }

        if (news.getScope() == NotificationScope.EXTERNAL) {
            if (news.getTargetBuildingId() == null) {
                return true;
            }

            UUID residentBuildingId = getResidentBuildingId(residentId);
            if (residentBuildingId == null) {
                log.debug("Unable to resolve building for resident {} -> allow news {}", residentId, news.getId());
                return true;
            }
            return residentBuildingId.equals(news.getTargetBuildingId());
        }

        return true;
    }

    private UUID getResidentBuildingId(UUID residentId) {
        if (residentId == null) {
            return null;
        }
        try {
            List<HouseholdMemberDto> members = baseServiceClient.getActiveHouseholdMembersByResident(residentId);
            if (members == null || members.isEmpty()) {
                return null;
            }

            HouseholdMemberDto prioritizedMember = members.stream()
                    .filter(member -> Boolean.TRUE.equals(member.isPrimary()))
                    .findFirst()
                    .orElse(members.get(0));

            if (prioritizedMember.householdId() == null) {
                return null;
            }

            HouseholdDto household = baseServiceClient.getHouseholdById(prioritizedMember.householdId());
            if (household == null || household.unitId() == null) {
                return null;
            }

            UnitDto unit = baseServiceClient.getUnitById(household.unitId());
            if (unit == null) {
                return null;
            }

            return unit.buildingId();
        } catch (Exception e) {
            log.warn("Failed to resolve building for resident {}: {}", residentId, e.getMessage());
            return null;
        }
    }

    private void validateNewsScope(NotificationScope scope, String targetRole, UUID targetBuildingId) {
        if (scope == null) {
            return;
        }

        if (scope == NotificationScope.INTERNAL) {
            if (targetRole == null || targetRole.isBlank()) {
                throw new IllegalArgumentException("INTERNAL news must have target_role (use 'ALL' for all roles)");
            }
            if (targetBuildingId != null) {
                throw new IllegalArgumentException("INTERNAL news cannot have target_building_id");
            }
        } else if (scope == NotificationScope.EXTERNAL) {
            if (targetRole != null && !targetRole.isBlank()) {
                throw new IllegalArgumentException("EXTERNAL news cannot have target_role");
            }
        }
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
                .scope(news.getScope())
                .targetRole(news.getTargetRole())
                .targetBuildingId(news.getTargetBuildingId())
                .viewCount(news.getViewCount())
                .images(toImageDtos(news.getImages()))
                .createdBy(news.getCreatedBy())
                .createdAt(news.getCreatedAt())
                .updatedBy(news.getUpdatedBy())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}