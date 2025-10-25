-- Xóa tất cả bảng liên quan đến News, NewsAttachment, NewsCategory, NewsRead
DROP TABLE IF EXISTS qhomebaseapp.news_read CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news_attachment CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news CASCADE;
DROP TABLE IF EXISTS qhomebaseapp.news_category CASCADE;
-- V28__create_news_tables.sql
CREATE TABLE qhomebaseapp.news_category (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL
);

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

CREATE TABLE qhomebaseapp.news_attachment (
    id BIGSERIAL PRIMARY KEY,
    news_id BIGINT REFERENCES qhomebaseapp.news(id) ON DELETE CASCADE,
    filename VARCHAR(255),
    url VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE qhomebaseapp.news_read (
    user_id BIGINT NOT NULL,
    news_id BIGINT NOT NULL,
    read_at TIMESTAMP,
    PRIMARY KEY(user_id, news_id)
);
