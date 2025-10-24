package com.qhomebaseapp.dto.post;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDto {
    private Long id;
    private String content;
    private List<String> imageUrls;
    private String topic;
    private Long userId;
    private String userName;
    private String userAvatar;

    private int likeCount;
    private int commentCount;
    private int shareCount;

    private boolean likedByCurrentUser;
    private boolean canDelete;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
