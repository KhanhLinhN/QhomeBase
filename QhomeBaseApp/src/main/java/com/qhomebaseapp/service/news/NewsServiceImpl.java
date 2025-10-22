package com.qhomebaseapp.service.news;

import com.qhomebaseapp.dto.news.NewsDto;
import com.qhomebaseapp.mapper.NewsMapper;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsRead;
import com.qhomebaseapp.repository.news.NewsCategoryRepository;
import com.qhomebaseapp.repository.news.NewsReadRepository;
import com.qhomebaseapp.repository.news.NewsRepository;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsReadRepository newsReadRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsMapper newsMapper;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Page<NewsDto> listNews(String categoryCode, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<News> newsPage;

        if (categoryCode != null) {
            newsPage = newsRepository.findByCategory_CodeOrderByPublishedAtDesc(categoryCode, pageable);
        } else {
            newsPage = newsRepository.findAllByOrderByPublishedAtDesc(pageable);
        }

        return newsPage.map(n -> {
            boolean isRead = userId != null && newsReadRepository.existsByUserIdAndNewsId(userId, n.getId());
            return newsMapper.toDto(n, isRead);
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
            isRead = true;
        }

        return newsMapper.toDto(news, isRead);
    }

    @Override
    @Transactional
    public void markAsRead(Long newsId, Long userId) {
        boolean exists = newsReadRepository.existsByUserIdAndNewsId(userId, newsId);
        if (!exists) {
            NewsRead read = NewsRead.builder()
                    .userId(userId)
                    .newsId(newsId)
                    .readAt(LocalDateTime.now())
                    .build();
            newsReadRepository.save(read);

            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + userId,
                    Map.of(
                            "newsId", newsId,
                            "isRead", true
                    )
            );
        }
    }

    @Override
    public long unreadCount(Long userId) {
        return newsReadRepository.countUnreadByUserId(userId);
    }

    @Override
    public Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        String email = principal.getName();

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database."));
    }

    @Transactional
    public NewsDto createNews(News news) {
        News saved = newsRepository.save(news);

        // gửi real-time notification đến tất cả client đang subscribe
        messagingTemplate.convertAndSend("/topic/news", newsMapper.toDto(saved, false));

        return newsMapper.toDto(saved, false);
    }

    @Transactional
    public NewsDto updateNews(Long id, News updatedNews) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        news.setTitle(updatedNews.getTitle());
        news.setSummary(updatedNews.getSummary());
        news.setContent(updatedNews.getContent());
        news.setPublishedAt(updatedNews.getPublishedAt());
        news.setAttachments(updatedNews.getAttachments());

        News saved = newsRepository.save(news);

        messagingTemplate.convertAndSend("/topic/news", newsMapper.toDto(saved, false));

        return newsMapper.toDto(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsDto> listUnread(Long userId) {
        Pageable pageable = Pageable.unpaged();
        Page<News> unreadNewsPage = newsRepository.findUnreadNewsByUserId(userId, pageable);

        return unreadNewsPage.stream()
                .map(n -> newsMapper.toDto(n, false))
                .toList();
    }

}
