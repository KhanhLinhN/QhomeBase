package com.qhomebaseapp.repository.news;

import com.qhomebaseapp.model.NewsRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsReadRepository extends JpaRepository<NewsRead, NewsRead.Id> {
    @Query("SELECT COUNT(n) FROM News n WHERE n.visibleToAll = true " +
            "AND NOT EXISTS (SELECT 1 FROM NewsRead r WHERE r.newsId = n.id AND r.userId = :userId)")
    long countUnreadByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndNewsId(Long userId, Long newsId);
}
