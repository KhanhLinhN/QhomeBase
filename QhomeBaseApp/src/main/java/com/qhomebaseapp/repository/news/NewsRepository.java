package com.qhomebaseapp.repository.news;


import com.qhomebaseapp.model.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {

    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);

    List<News> findTop10ByPinnedTrueOrderByPublishedAtDesc();
    long countByVisibleToAllTrue();
    Page<News> findByCategory_CodeOrderByPublishedAtDesc(String categoryCode, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.visibleToAll = true " +
            "AND NOT EXISTS (SELECT 1 FROM NewsRead r WHERE r.newsId = n.id AND r.userId = :userId) " +
            "ORDER BY n.publishedAt DESC")
    Page<News> findUnreadNewsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.visibleToAll = true " +
            "AND EXISTS (SELECT 1 FROM NewsRead r WHERE r.newsId = n.id AND r.userId = :userId) " +
            "ORDER BY n.publishedAt DESC")
    Page<News> findReadNewsByUserId(@Param("userId") Long userId, Pageable pageable);
}
