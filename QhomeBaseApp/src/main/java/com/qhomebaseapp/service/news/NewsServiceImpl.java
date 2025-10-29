package com.qhomebaseapp.service.news;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.mapper.NewsMapper;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsRead;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.news.NewsReadRepository;
import com.qhomebaseapp.repository.news.NewsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
            return newsMapper.toDto(n);
        });
    }

    @Override
    public long unreadCount(Long userId) {
        return newsReadRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> listUnread(Long userId) {
        Pageable pageable = Pageable.unpaged();
        Page<News> unreadNewsPage = newsRepository.findUnreadNewsByUserId(userId, pageable);

        return unreadNewsPage.stream()
                .map(newsMapper::toDto)
                .toList();
    }

    @Override
    public Page<NewsDto> listNewsWithReadStatus(Long userId, Pageable pageable) {
        return newsRepository.findAllWithUserReadStatus(userId, pageable);
    }

    @Override
    public NewsDto getNewsByUuid(String uuid, Long userId) {
        News news;
        try {
            news = newsRepository.findByNewsUuid(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("News not found by UUID"));
        } catch (EntityNotFoundException e) {
            try {
                Long id = Long.parseLong(uuid);
                news = newsRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("News not found by id"));
            } catch (NumberFormatException ex) {
                throw e;
            }
        }
        return toDto(news, userId);
    }

    @Override
    @Transactional
    public void markAsReadByUuid(String uuidOrId, Long userId) {
        News news = newsRepository.findByNewsUuid(uuidOrId).orElse(null);
        if (news == null) {
            try {
                Long id = Long.parseLong(uuidOrId);
                news = newsRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("News not found by id"));
            } catch (NumberFormatException ex) {
                throw new EntityNotFoundException("News not found by UUID: " + uuidOrId);
            }
        }

        Optional<NewsRead> existing = newsReadRepository.findByUserIdAndNewsId(userId, news.getId());
        if (existing.isEmpty()) {
            NewsRead read = NewsRead.builder()
                    .userId(userId)
                    .newsId(news.getId())
                    .readAt(Instant.now())
                    .isRead(true)
                    .build();
            newsReadRepository.save(read);
        } else if (!existing.get().isRead()) {
            NewsRead read = existing.get();
            read.setIsRead(true);
            read.setReadAt(Instant.now());
            newsReadRepository.save(read);
        }

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                Map.of(
                        "newsId", news.getId(),
                        "newsUuid", news.getNewsUuid(),
                        "isRead", true
                )
        );
    }

    private NewsDto toDto(News news, Long userId) {
        NewsDto dto = new NewsDto();
        dto.setId(news.getId());
        dto.setNewsUuid(news.getNewsUuid());
        dto.setTitle(news.getTitle());
        dto.setSummary(news.getSummary());
        dto.setCoverImageUrl(news.getCoverImageUrl());
        dto.setDeepLink(news.getDeepLink());
        dto.setStatus(news.getStatus());
        dto.setPublishAt(news.getPublishAt());
        dto.setReceivedAt(news.getReceivedAt());
        dto.setCreatedAt(news.getCreatedAt());
        dto.setUpdatedAt(news.getUpdatedAt());
        return dto;
    }
}