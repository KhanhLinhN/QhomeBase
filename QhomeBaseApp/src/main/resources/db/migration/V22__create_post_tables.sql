-- ================================
-- V22__create_post_tables.sql
-- ================================
CREATE TABLE IF NOT EXISTS qhomebaseapp.posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    image_urls TEXT[], -- danh sách ảnh (nếu có)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    share_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS qhomebaseapp.post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS qhomebaseapp.post_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
