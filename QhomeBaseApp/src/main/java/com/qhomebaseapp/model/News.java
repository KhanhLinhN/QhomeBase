package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "news", schema = "qhomebaseapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "news_uuid", unique = true, nullable = false)
    private String newsUuid;

    private String title;
    private String summary;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(name = "status")
    private String status;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "expire_at")
    private Instant expireAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "raw_payload")
    private String rawPayload;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
