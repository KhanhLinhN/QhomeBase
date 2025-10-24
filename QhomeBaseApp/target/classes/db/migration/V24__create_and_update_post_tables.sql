-- =========================================
-- V24__create_and_update_post_tables.sql
-- Migration dành cho hệ thống bài viết (Post, Comment, Like, Share)
-- =========================================

-- =========================================
-- 1️⃣ Cập nhật bảng posts: thêm cột topic
-- =========================================
ALTER TABLE qhomebaseapp.posts ADD COLUMN topic VARCHAR(100);

-- Index hỗ trợ phân trang & tìm kiếm
CREATE INDEX IF NOT EXISTS idx_posts_user_id ON qhomebaseapp.posts(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_topic ON qhomebaseapp.posts(topic);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON qhomebaseapp.posts(created_at DESC);

-- =========================================
-- 2️⃣ BẢNG post_images (dành cho @ElementCollection)
-- =========================================
CREATE TABLE IF NOT EXISTS qhomebaseapp.post_images (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    url TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_post_images_post_id ON qhomebaseapp.post_images(post_id);

-- =========================================
-- 3️⃣ BẢNG post_comments
-- =========================================
CREATE TABLE IF NOT EXISTS qhomebaseapp.post_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_post_comments_post_id ON qhomebaseapp.post_comments(post_id);

-- =========================================
-- 4️⃣ BẢNG post_likes
-- =========================================
CREATE TABLE IF NOT EXISTS qhomebaseapp.post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_post_like UNIQUE (post_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON qhomebaseapp.post_likes(post_id);

-- =========================================
-- 5️⃣ BẢNG post_shares
-- =========================================
CREATE TABLE IF NOT EXISTS qhomebaseapp.post_shares (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_post_shares_post_id ON qhomebaseapp.post_shares(post_id);