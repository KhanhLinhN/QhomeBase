package com.qhomebaseapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_images", schema = "qhomebaseapp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private String url;
}
