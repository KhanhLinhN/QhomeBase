package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    private Long getAuthenticatedUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }

    @GetMapping
    public ResponseEntity<Page<NewsDto>> list(
            @RequestParam(value = "category", required = false) String categoryCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long userId = authentication != null ? getAuthenticatedUserId(authentication) : null;
        Page<NewsDto> result = newsService.listNews(categoryCode, userId, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> get(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = authentication != null ? getAuthenticatedUserId(authentication) : null;
        NewsDto dto = newsService.getNews(id, userId);
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
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> unreadCount(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        long count = newsService.unreadCount(userId);
        return ResponseEntity.ok(count);
    }

}
