-- V4__create_news_tables.sql
-- Schema should already exist (created by earlier migrations)
CREATE SCHEMA IF NOT EXISTS qhomebaseapp;

-- Loại tin (category/type): BQT/BQL/INTERNAL/GOV
CREATE TABLE IF NOT EXISTS qhomebaseapp.news_category (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE, -- e.g. BQT, BQL, INTERNAL, GOV
    name VARCHAR(255) NOT NULL
);

-- Bảng tin chính
CREATE TABLE IF NOT EXISTS qhomebaseapp.news (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES qhomebaseapp.news_category(id) ON DELETE SET NULL,
    title VARCHAR(500) NOT NULL,
    summary VARCHAR(1000),
    content TEXT NOT NULL,
    author VARCHAR(255),
    source VARCHAR(255),
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pinned BOOLEAN DEFAULT FALSE,
    visible_to_all BOOLEAN DEFAULT TRUE, -- nếu false thì có thể chỉ visible to specific roles
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP
);

-- Đánh dấu đã đọc: mapping giữa user và news
CREATE TABLE IF NOT EXISTS qhomebaseapp.news_read (
    user_id BIGINT NOT NULL,
    news_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, news_id),
    CONSTRAINT fk_nr_user FOREIGN KEY (user_id) REFERENCES qhomebaseapp.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_nr_news FOREIGN KEY (news_id) REFERENCES qhomebaseapp.news (id) ON DELETE CASCADE
);

-- Optional: attachments (url)
CREATE TABLE IF NOT EXISTS qhomebaseapp.news_attachment (
    id BIGSERIAL PRIMARY KEY,
    news_id BIGINT REFERENCES qhomebaseapp.news(id) ON DELETE CASCADE,
    filename VARCHAR(255),
    url VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed categories
INSERT INTO qhomebaseapp.news_category(code, name)
VALUES
  ('BQT','Ban Quản Trị'),
  ('BQL','Ban Quản Lý'),
  ('INTERNAL','Thông tin nội bộ'),
  ('GOV','Thông tin chính quyền')
ON CONFLICT (code) DO NOTHING;
