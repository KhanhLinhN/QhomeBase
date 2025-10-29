package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NewsDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishAt").descending());
        Page<NewsDto> result = newsService.listNewsWithReadStatus(userId, pageable);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/{uuid}")
    public ResponseEntity<NewsDto> get(
            @PathVariable String uuid,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        try {
            log.info("Fetching news {} for user {}", uuid, userId);
            NewsDto dto = newsService.getNewsByUuid(uuid, userId);
            log.info("Fetched news: {}", dto);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error fetching news {} for user {}: {}", uuid, userId, e.getMessage(), e);
            return ResponseEntity.status(404).body(null);
        }
    }

    @PostMapping("/{uuid}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(
            @PathVariable String uuid,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        try {
            log.info("Marking news {} as read for user {}", uuid, userId);
            newsService.markAsReadByUuid(uuid, userId);
            log.info("Marked news {} as read successfully", uuid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking news {} as read for user {}: {}", uuid, userId, e.getMessage(), e);
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> unreadCount(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        long count = newsService.unreadCount(userId);

        log.info("User {} requested unread news count: {}", userId, count);

        return ResponseEntity.ok(count);
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NewsDto>> getUnread(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        List<NewsDto> unreadNews = newsService.listUnread(userId);
        return ResponseEntity.ok(unreadNews);
    }
}
