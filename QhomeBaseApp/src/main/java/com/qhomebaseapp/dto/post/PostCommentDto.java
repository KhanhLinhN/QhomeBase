package com.qhomebaseapp.dto.post;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostCommentDto {
    private Long id;
    private Long postId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
    private Long parentId;
    private List<PostCommentDto> replies;
}
