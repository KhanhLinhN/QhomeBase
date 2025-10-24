package com.qhomebaseapp.dto.post;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostLikeDto {
    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}