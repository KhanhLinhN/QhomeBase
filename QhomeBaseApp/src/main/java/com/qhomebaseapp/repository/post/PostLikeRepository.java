package com.qhomebaseapp.repository.post;
import com.qhomebaseapp.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    Long countByPostId(Long postId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}