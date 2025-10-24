package com.qhomebaseapp.repository.post;

import com.qhomebaseapp.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Post> findByTopicContainingIgnoreCase(String topic, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCaseAndTopicContainingIgnoreCase(String keyword, String topic, Pageable pageable);
}
