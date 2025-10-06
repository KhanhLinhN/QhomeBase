package com.qhomebaseapp.controller;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.service.news.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<Page<NewsDto>> list(
            @RequestParam(value = "category", required = false) String categoryCode,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Page<NewsDto> result = newsService.listNews(categoryCode, userId, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> get(
            @PathVariable Long id,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        NewsDto dto = newsService.getNews(id, userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @RequestParam("userId") Long userId
    ) {
        newsService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@RequestParam("userId") Long userId) {
        long count = newsService.unreadCount(userId);
        return ResponseEntity.ok(count);
    }

}
