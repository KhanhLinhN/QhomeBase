package com.qhomebaseapp.model;

import jakarta.persistence.*;

@Entity
public class NewsImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    private News news;

    private String url;
    private String caption;
    private Integer sortOrder;
}
