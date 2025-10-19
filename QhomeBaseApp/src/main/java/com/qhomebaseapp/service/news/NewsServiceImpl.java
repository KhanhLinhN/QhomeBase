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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsReadRepository newsReadRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsMapper newsMapper;
    private final UserRepository userRepository;

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
}
