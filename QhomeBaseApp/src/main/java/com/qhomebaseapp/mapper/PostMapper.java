package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.post.PostCommentDto;
import com.qhomebaseapp.dto.post.PostDto;
import com.qhomebaseapp.dto.post.PostLikeDto;
import com.qhomebaseapp.model.Post;
import com.qhomebaseapp.model.PostComment;
import com.qhomebaseapp.model.PostImage;
import com.qhomebaseapp.model.PostLike;
import com.qhomebaseapp.repository.post.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private final PostLikeRepository likeRepository;

    public PostDto toDto(Post post, Long currentUserId) {
        PostDto dto = new PostDto();

        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setTopic(post.getTopic());
        dto.setUserId(post.getUser().getId());
        dto.setUserName(post.getUser().getFullName());
        dto.setUserAvatar(post.getUser().getAvatarUrl());
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setShareCount(post.getShareCount());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        if (post.getImages() != null && !post.getImages().isEmpty()) {
            List<String> urls = post.getImages().stream()
                    .map(PostImage::getUrl)
                    .collect(Collectors.toList());
            dto.setImageUrls(urls);
        } else {
            dto.setImageUrls(Collections.emptyList());
        }

        if (currentUserId != null) {
            dto.setLikedByCurrentUser(
                    likeRepository.existsByPostIdAndUserId(post.getId(), currentUserId)
            );
            dto.setCanDelete(post.getUser().getId().equals(currentUserId));
        } else {
            dto.setLikedByCurrentUser(false);
            dto.setCanDelete(false);
        }

        return dto;
    }

    public List<PostDto> toDtoList(List<Post> posts, Long currentUserId) {
        return posts.stream()
                .map(post -> toDto(post, currentUserId))
                .collect(Collectors.toList());
    }

    public PostCommentDto toDto(PostComment comment) {
        if (comment == null) return null;

        PostCommentDto dto = new PostCommentDto();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPost().getId());
        dto.setUserId(comment.getUser().getId());
        dto.setUserName(comment.getUser().getFullName());
        dto.setUserAvatar(comment.getUser().getAvatarUrl());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);

        return dto;
    }

    public PostLikeDto toDto(PostLike like) {
        PostLikeDto dto = new PostLikeDto();
        dto.setId(like.getId());
        dto.setPostId(like.getPost().getId());
        dto.setUserId(like.getUser().getId());
        dto.setCreatedAt(like.getCreatedAt());
        return dto;
    }
}
