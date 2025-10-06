package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // <-- Cần import mới này

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    private Long getAuthenticatedUserId(Principal principal) {
        return newsService.getUserIdFromPrincipal(principal);
    }

    @GetMapping
    public ResponseEntity<Page<NewsDto>> list(
            @RequestParam(value = "category", required = false) String categoryCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Principal principal
    ) {
        Long userId = principal != null ? getAuthenticatedUserId(principal) : null;

        Page<NewsDto> result = newsService.listNews(categoryCode, userId, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> get(
            @PathVariable Long id,
            Principal principal
    ) {
        Long userId = principal != null ? getAuthenticatedUserId(principal) : null;
        NewsDto dto = newsService.getNews(id, userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = getAuthenticatedUserId(principal);
        newsService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = getAuthenticatedUserId(principal);
        long count = newsService.unreadCount(userId);
        return ResponseEntity.ok(count);
    }

}
