package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "news_views", schema = "content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Enumerated(EnumType.STRING)
    @Column(name = "viewer_type", nullable = false, columnDefinition = "content.viewer_type")
    @Builder.Default
    private ViewerType viewerType = ViewerType.RESIDENT;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant viewedAt = Instant.now();


    @PrePersist
    @PreUpdate
    private void validate() {
        boolean hasResident = residentId != null;
        boolean hasUser = userId != null;

        if (hasResident == hasUser) {
            throw new IllegalStateException(
                "NewsView must have exactly one of residentId or userId"
            );
        }


        if (hasResident) {
            viewerType = ViewerType.RESIDENT;
        } else {
            viewerType = ViewerType.USER;
        }
    }


    public static NewsView forResident(News news, UUID residentId) {
        return NewsView.builder()
                .news(news)
                .viewerType(ViewerType.RESIDENT)
                .residentId(residentId)
                .build();
    }


    public static NewsView forUser(News news, UUID userId) {
        return NewsView.builder()
                .news(news)
                .viewerType(ViewerType.USER)
                .userId(userId)
                .build();
    }
}


