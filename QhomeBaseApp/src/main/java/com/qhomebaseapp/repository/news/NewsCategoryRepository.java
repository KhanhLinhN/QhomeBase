package com.qhomebaseapp.repository.news;

import com.qhomebaseapp.model.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Long> {
    NewsCategory findByCode(String code);
}