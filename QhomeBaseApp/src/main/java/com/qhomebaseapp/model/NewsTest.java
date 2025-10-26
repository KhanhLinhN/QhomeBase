package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "qhomebaseapp", name = "news_test")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsTest {
    @Id
    private UUID id;

    private String title;
    private String summary;
    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;
}
