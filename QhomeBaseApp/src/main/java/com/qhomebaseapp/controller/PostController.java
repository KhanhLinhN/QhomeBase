package com.qhomebaseapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qhomebaseapp.dto.post.*;
import com.qhomebaseapp.mapper.PostMapper;
import com.qhomebaseapp.model.Post;
import com.qhomebaseapp.model.PostComment;
import com.qhomebaseapp.model.PostImage;
import com.qhomebaseapp.model.PostLike;
import com.qhomebaseapp.security.CustomUserDetails;
import com.qhomebaseapp.service.post.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PostMapper postMapper;

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<PostDto> createPost(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            CreatePostRequest request = mapper.readValue(requestJson, CreatePostRequest.class);

            List<String> uploadedUrls = postService.handleFileUploads(files);

            Post post = postService.createPost(userId, request.getContent(), uploadedUrls, request.getTopic());

            List<String> imageUrls = post.getImages().stream()
                    .map(PostImage::getUrl)
                    .toList();

            PostDto dto = postMapper.toDto(post, userId);
            dto.setImageUrls(imageUrls);

            messagingTemplate.convertAndSend("/topic/posts", dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (Exception e) {
            log.error("Error creating post", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topic,
            Authentication authentication
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postService.getAllPosts(pageable, keyword, topic);

        Long userId = getAuthenticatedUserId(authentication);
        List<PostDto> postDtos = postMapper.toDtoList(postPage.getContent(), userId);

        return ResponseEntity.ok(Map.of(
                "posts", postDtos,
                "currentPage", postPage.getNumber(),
                "totalItems", postPage.getTotalElements(),
                "totalPages", postPage.getTotalPages()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PostDto>> getMyPosts(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Post> posts = postService.getPostsByUser(userId);
        List<PostDto> postDtos = postMapper.toDtoList(posts, userId);

        return ResponseEntity.ok(postDtos);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeDto> likePost(@PathVariable Long postId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PostLike like = postService.likePost(postId, userId);
        PostLikeDto dto = postMapper.toDto(like);

        try {
            messagingTemplate.convertAndSend("/topic/posts/likes", dto);
        } catch (Exception e) {
            log.warn("WebSocket send failed on like: postId={}", postId, e);
        }

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        postService.unlikePost(postId, userId);
        try {
            messagingTemplate.convertAndSend("/topic/posts/unlikes", Map.of("postId", postId, "userId", userId));
        } catch (Exception e) {
            log.warn("WebSocket send failed on unlike: postId={}", postId, e);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<PostCommentDto> commentPost(@PathVariable Long postId,
                                                      @Valid @RequestBody CreateCommentRequest request,
                                                      Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PostComment comment = postService.addComment(postId, userId, request.getContent());
        PostCommentDto dto = postMapper.toDto(comment);

        try {
            messagingTemplate.convertAndSend("/topic/posts/comments", dto);
        } catch (Exception e) {
            log.warn("WebSocket send failed on comment: postId={}", postId, e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        postService.deleteComment(commentId, userId);
        try {
            messagingTemplate.convertAndSend("/topic/posts/comments/deletes",
                    Map.of("commentId", commentId, "userId", userId));
        } catch (Exception e) {
            log.warn("WebSocket send failed on deleteComment: {}", commentId, e);
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        postService.deletePostSafely(postId, userId);
        try {
            messagingTemplate.convertAndSend("/topic/posts/deletes", Map.of("postId", postId, "userId", userId));
        } catch (Exception e) {
            log.warn("WebSocket send failed on deletePost: {}", postId, e);
        }

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{postId}/comments/{commentId}/reply")
    public ResponseEntity<PostCommentDto> replyToComment(@PathVariable Long postId,
                                                         @PathVariable Long commentId,
                                                         @Valid @RequestBody CreateCommentRequest request,
                                                         Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PostComment reply = postService.replyToComment(postId, commentId, userId, request.getContent());
        PostCommentDto dto = postMapper.toDto(reply);

        try {
            messagingTemplate.convertAndSend("/topic/posts/comments/replies", dto);
        } catch (Exception e) {
            log.warn("WebSocket send failed on reply: postId={}, commentId={}", postId, commentId, e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<PostCommentDto>> getCommentsByPost(@PathVariable Long postId) {
        List<PostComment> comments = postService.getCommentsByPost(postId);

        List<PostCommentDto> dtos = comments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> {
                    PostCommentDto dto = postMapper.toDto(c);
                    List<PostCommentDto> replies = comments.stream()
                            .filter(r -> r.getParent() != null && r.getParent().getId().equals(c.getId()))
                            .map(postMapper::toDto)
                            .collect(Collectors.toList());
                    dto.setReplies(replies);
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<PostCommentDto>> getReplies(@PathVariable Long commentId) {
        List<PostComment> replies = postService.getRepliesByCommentId(commentId);
        List<PostCommentDto> dtos = replies.stream()
                .map(postMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<Void> sharePost(@PathVariable Long postId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        postService.sharePost(postId, userId);
        try {
            messagingTemplate.convertAndSend("/topic/posts/shares", Map.of("postId", postId, "userId", userId));
        } catch (Exception e) {
            log.warn("WebSocket send failed on share: postId={}", postId, e);
        }

        return ResponseEntity.noContent().build();
    }

}
