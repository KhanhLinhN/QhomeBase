package com.qhomebaseapp.repository.post;

import com.qhomebaseapp.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    Long countByPostId(Long postId);
    List<PostComment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}