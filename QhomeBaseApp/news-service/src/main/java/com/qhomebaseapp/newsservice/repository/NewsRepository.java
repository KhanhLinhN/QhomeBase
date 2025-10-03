package com.qhomebaseapp.newsservice.repository;

import com.qhomebaseapp.newsservice.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
}
