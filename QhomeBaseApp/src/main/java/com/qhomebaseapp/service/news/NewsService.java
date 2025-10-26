package com.qhomebaseapp.service.news;

import com.qhomebaseapp.dto.news.NewsDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.security.Principal; // Import cần thiết
import java.util.List;

@Service
public interface NewsService {
    NewsDto getNewsByUuid(String uuid, Long userId);
    void markAsReadByUuid(String uuid, Long userId);
    Page<NewsDto> listNews(String categoryCode, Long userId, int page, int size);
    void markAsRead(Long newsId, Long userId);
    long unreadCount(Long userId);
    List<NewsDto> listUnread(Long userId);
}
