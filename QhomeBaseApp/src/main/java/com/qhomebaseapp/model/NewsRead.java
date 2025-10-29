package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "news_read", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(NewsRead.Id.class)
public class NewsRead {
    @jakarta.persistence.Id
    @Column(name = "user_id")
    private Long userId;

    @jakarta.persistence.Id
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    public void setIsRead(boolean read) {
        isRead = read;
    }

    public static class Id implements Serializable {
        private Long userId;
        private Long newsId;

        public Id() {}
        public Id(Long userId, Long newsId) {
            this.userId = userId;
            this.newsId = newsId;
        }
    }
}