package com.qhomebaseapp.dto.post;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class LikeDto {
    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}