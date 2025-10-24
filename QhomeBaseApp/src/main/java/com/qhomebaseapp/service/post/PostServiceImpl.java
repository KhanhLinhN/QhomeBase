package com.qhomebaseapp.service.post;

import com.qhomebaseapp.model.*;
import com.qhomebaseapp.repository.UserRepository;
import com.qhomebaseapp.repository.post.*;
import com.qhomebaseapp.service.storage.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final PostShareRepository shareRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PostImageRepository postImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Post> getAllPosts(Pageable pageable, String keyword, String topic) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasTopic = topic != null && !topic.isBlank();

        if (hasKeyword && hasTopic) {
            return postRepository.findByContentContainingIgnoreCaseAndTopicContainingIgnoreCase(keyword.trim(), topic.trim(), pageable);
        } else if (hasKeyword) {
            return postRepository.findByContentContainingIgnoreCase(keyword.trim(), pageable);
        } else if (hasTopic) {
            return postRepository.findByTopicContainingIgnoreCase(topic.trim(), pageable);
        } else {
            return postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByUser(Long userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<String> handleFileUploads(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        try {
            return fileStorageService.uploadMultiple(files);
        } catch (Exception e) {
            throw new RuntimeException("Upload file thất bại", e);
        }
    }

    @Override
    @Transactional
    public void deletePostSafely(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết ID: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Không được phép xóa bài viết của người khác");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        deletePostSafely(postId, userId);
    }

    @Override
    @Transactional
    public PostComment addComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết ID: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId));

        PostComment comment = PostComment.builder()
                .post(post)
                .user(user)
                .content(content)
                .build();

        commentRepository.save(comment);

        post.setCommentCount(commentRepository.countByPostId(postId).intValue());
        postRepository.save(post);

        return comment;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bình luận ID: " + commentId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Không được phép xóa bình luận của người khác");
        }

        Post post = comment.getPost();
        commentRepository.delete(comment);

        post.setCommentCount(commentRepository.countByPostId(post.getId()).intValue());
        postRepository.save(post);
    }

    @Override
    @Transactional
    public PostLike likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết ID: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId));

        return likeRepository.findByPostIdAndUserId(postId, userId)
                .orElseGet(() -> {
                    PostLike newLike = PostLike.builder().post(post).user(user).build();
                    likeRepository.save(newLike);
                    post.setLikeCount(likeRepository.countByPostId(postId).intValue());
                    postRepository.save(post);
                    return newLike;
                });
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        PostLike like = likeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lượt like"));

        likeRepository.delete(like);

        Post post = like.getPost();
        post.setLikeCount(likeRepository.countByPostId(postId).intValue());
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void sharePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết ID: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId));

        PostShare share = PostShare.builder().post(post).user(user).build();
        shareRepository.save(share);

        post.setShareCount(shareRepository.countByPostId(postId).intValue());
        postRepository.save(post);
    }

    @Override
    @Transactional
    public PostComment replyToComment(Long postId, Long parentCommentId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bài viết ID: " + postId));
        PostComment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bình luận cha ID: " + parentCommentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId));

        PostComment reply = PostComment.builder()
                .post(post)
                .user(user)
                .content(content)
                .parent(parent)
                .build();

        commentRepository.save(reply);

        post.setCommentCount(commentRepository.countByPostId(postId).intValue());
        postRepository.save(post);

        return reply;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostComment> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostComment> getRepliesByCommentId(Long commentId) {
        PostComment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bình luận cha ID: " + commentId));
        return commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
    }

    @Override
    @Transactional
    public Post createPost(Long userId, String content, List<String> imageUrls, String topic) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng ID: " + userId));

        Post post = Post.builder()
                .user(user)
                .content(content)
                .topic(topic)  // set topic
                .likeCount(0)
                .commentCount(0)
                .shareCount(0)
                .build();

        post.setImages(new ArrayList<>());
        postRepository.save(post);

        if (imageUrls != null) {
            for (String url : imageUrls) {
                PostImage img = new PostImage();
                img.setPost(post);
                img.setUrl(url);
                post.getImages().add(img);
                postImageRepository.save(img);
            }
        }

        return post;
    }

}
