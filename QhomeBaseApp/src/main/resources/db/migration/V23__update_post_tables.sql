-- db/migration/V23__update_post_tables.sql

-- Thêm bảng share nếu cần
CREATE TABLE IF NOT EXISTS qhomebaseapp.post_shares (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES qhomebaseapp.posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES qhomebaseapp.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Thêm cột updated_at cho post_comments
ALTER TABLE qhomebaseapp.post_comments
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

-- Thêm cột updated_at cho post_likes (nếu muốn tracking)
ALTER TABLE qhomebaseapp.post_likes
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();
