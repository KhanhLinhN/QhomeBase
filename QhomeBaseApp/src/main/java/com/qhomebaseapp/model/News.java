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
    //    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "category_id")
//    private NewsCategory category;
//
//    private String title;
//    private String summary;
//
//    @Column(columnDefinition = "text")
//    private String content;
//
//    private String author;
//    private String source;
//
//    private LocalDateTime publishedAt;
//    private Boolean pinned;
//    private Boolean visibleToAll;
//
//    private String createdBy;
//    private LocalDateTime createdAt;
//    private String updatedBy;
//    private LocalDateTime updatedAt;
//
//    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<NewsAttachment> attachments;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "news_uuid", unique = true, nullable = false)
    private UUID newsUuid; // UUID từ web admin

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

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
