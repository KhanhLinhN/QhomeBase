package com.qhomebaseapp.service.news;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.mapper.NewsMapper;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsRead;
import com.qhomebaseapp.model.User;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.news.NewsReadRepository;
import com.qhomebaseapp.repository.news.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsReadRepository newsReadRepository;
    private final NewsMapper newsMapper;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Page<NewsDto> listNews(String categoryCode, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishAt"));
        Page<News> newsPage = newsRepository.findAllByOrderByPublishAtDesc(pageable);

        return newsPage.map(n -> {
            boolean isRead = userId != null && newsReadRepository.existsByUserIdAndNewsId(userId, n.getId());
            n.setIsRead(isRead);
            return newsMapper.toDto(n);
        });
    }

    @Override
    @Transactional
    public NewsDto getNews(Long id, Long userId) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        boolean isRead = userId != null && newsReadRepository.existsByUserIdAndNewsId(userId, news.getId());

        if (userId != null && !isRead) {
            markAsRead(news.getId(), userId);
            news.setIsRead(true);
        } else {
            news.setIsRead(isRead);
        }

        return newsMapper.toDto(news);
    }

    @Override
    @Transactional
    public void markAsRead(Long newsId, Long userId) {
        boolean exists = newsReadRepository.existsByUserIdAndNewsId(userId, newsId);
        if (!exists) {
            NewsRead read = NewsRead.builder()
                    .userId(userId)
                    .newsId(newsId)
                    .readAt(Instant.now())
                    .build();
            newsReadRepository.save(read);

            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + userId,
                    Map.of("newsId", newsId, "isRead", true)
            );
        }
    }

    @Override
    public long unreadCount(Long userId) {
        return newsReadRepository.countUnreadByUserId(userId);
    }

    @Override
    public Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) return null;
        String email = principal.getName();

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
    }

    @Transactional
    public NewsDto createNews(News news) {
        news.setReceivedAt(news.getReceivedAt() != null ? news.getReceivedAt() : Instant.now());
        news.setCreatedAt(Instant.now());
        news.setUpdatedAt(Instant.now());
        news.setIsRead(false);

        News saved = newsRepository.save(news);

        messagingTemplate.convertAndSend("/topic/news", newsMapper.toDto(saved));

        return newsMapper.toDto(saved);
    }

    @Transactional
    public NewsDto updateNews(Long id, News updatedNews) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        news.setTitle(updatedNews.getTitle());
        news.setSummary(updatedNews.getSummary());
        news.setBodyHtml(updatedNews.getBodyHtml());
        news.setCoverImageUrl(updatedNews.getCoverImageUrl());
        news.setDeepLink(updatedNews.getDeepLink());
        news.setStatus(updatedNews.getStatus());
        news.setPublishAt(updatedNews.getPublishAt());
        news.setExpireAt(updatedNews.getExpireAt());
        news.setRawPayload(updatedNews.getRawPayload());
        news.setUpdatedAt(Instant.now());

        News saved = newsRepository.save(news);

        messagingTemplate.convertAndSend("/topic/news", newsMapper.toDto(saved));

        return newsMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> listUnread(Long userId) {
        Pageable pageable = Pageable.unpaged();
        Page<News> unreadNewsPage = newsRepository.findUnreadNewsByUserId(userId, pageable);

        return unreadNewsPage.stream()
                .peek(n -> n.setIsRead(false))
                .map(newsMapper::toDto)
                .toList();
    }
}