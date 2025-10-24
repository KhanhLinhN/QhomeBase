package com.qhomebaseapp.service.post;

import com.qhomebaseapp.model.Post;
import com.qhomebaseapp.model.PostComment;
import com.qhomebaseapp.model.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {
    Post createPost(Long userId, String content, List<String> imageUrls, String topic);
    List<Post> getPostsByUser(Long userId);

    Page<Post> getAllPosts(Pageable pageable, String keyword, String topic);

    void deletePostSafely(Long postId, Long userId);

    void deletePost(Long postId, Long userId);

    List<String> handleFileUploads(List<MultipartFile> files);

    PostComment addComment(Long postId, Long userId, String content);
    void deleteComment(Long commentId, Long userId);

    PostLike likePost(Long postId, Long userId);
    void unlikePost(Long postId, Long userId);

    void sharePost(Long postId, Long userId);
    PostComment replyToComment(Long postId, Long parentCommentId, Long userId, String content);

    List<PostComment> getCommentsByPost(Long postId);
    List<PostComment> getRepliesByCommentId(Long commentId);

}
