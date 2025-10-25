-- Drop bảng news và các phụ thuộc nếu có
DROP TABLE IF EXISTS qhomebaseapp.news CASCADE;
-- V28__create_news_table.sql
CREATE TABLE qhomebaseapp.news (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES qhomebaseapp.news_category(id) ON DELETE SET NULL,
    title VARCHAR(255),
    summary VARCHAR(255),
    content TEXT,
    author VARCHAR(255),
    source VARCHAR(255),
    published_at TIMESTAMP,
    pinned BOOLEAN,
    visible_to_all BOOLEAN,
    created_by VARCHAR(255),
    created_at TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP
);
