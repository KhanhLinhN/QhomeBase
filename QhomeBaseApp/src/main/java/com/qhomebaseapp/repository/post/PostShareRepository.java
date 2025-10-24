package com.qhomebaseapp.repository.post;

import com.qhomebaseapp.model.PostShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostShareRepository extends JpaRepository<PostShare, Long> {
    Long countByPostId(Long postId);
}
