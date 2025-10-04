package com.qhomebaseapp.service.news;

import com.qhomebaseapp.dto.CreateNewsRequest;
import com.qhomebaseapp.dto.NewsDto;
import com.qhomebaseapp.dto.UpdateNewsRequest;
import com.qhomebaseapp.mapper.NewsMapper;
import com.qhomebaseapp.model.News;
import com.qhomebaseapp.model.NewsAttachment;
import com.qhomebaseapp.model.NewsCategory;
import com.qhomebaseapp.model.NewsRead;
import com.qhomebaseapp.repository.news.NewsCategoryRepository;
import com.qhomebaseapp.repository.news.NewsReadRepository;
import com.qhomebaseapp.repository.news.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsReadRepository newsReadRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsMapper newsMapper;

    /**
     * Danh sách tin tức (cư dân xem), có phân trang và filter theo category
     */
    @Override
    public Page<NewsDto> listNews(String categoryCode, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
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

    /**
     * Xem chi tiết tin tức -> đồng thời đánh dấu là đã đọc
     */
    @Override
    @Transactional
    public NewsDto getNews(Long id, Long userId) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));

        boolean isRead = userId != null && newsReadRepository.existsByUserIdAndNewsId(userId, news.getId());

        // Nếu user chưa đọc thì đánh dấu là đã đọc khi mở tin
        if (userId != null && !isRead) {
            markAsRead(news.getId(), userId);
            isRead = true;
        }

        return newsMapper.toDto(news, isRead);
    }

    /**
     * Đánh dấu 1 tin tức là đã đọc
     */
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

    /**
     * Đếm số tin chưa đọc để hiển thị trên icon notification
     */
    @Override
    public long unreadCount(Long userId) {
        return newsReadRepository.countUnreadByUserId(userId);
    }
}
