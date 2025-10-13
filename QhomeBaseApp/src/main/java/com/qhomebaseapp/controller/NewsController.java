package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Page<NewsDto>> list(
            @RequestParam(value = "category", required = false) String categoryCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        Page<NewsDto> result = newsService.listNews(categoryCode, userId, page, size);

        log.info("User {} listed news, category={}, page={}, size={}, count={}",
                userId, categoryCode, page, size, result.getTotalElements());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> get(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        NewsDto dto = newsService.getNews(id, userId);

        log.info("User {} fetched news {}", userId, id);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        newsService.markAsRead(id, userId);

        log.info("User {} marked news {} as read", userId, id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> unreadCount(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        long count = newsService.unreadCount(userId);

        log.info("User {} requested unread news count: {}", userId, count);

        return ResponseEntity.ok(count);
    }
}
