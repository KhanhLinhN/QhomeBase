package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    private LocalDateTime readAt;

    public static class Id implements Serializable {
        private Long userId;
        private Long newsId;
        public Id() {}
        public Id(Long userId, Long newsId) { this.userId = userId; this.newsId = newsId; }

    }
}
