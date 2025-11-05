-- ================================
-- V43__drop_post_tables.sql
-- Xóa tất cả các bảng liên quan đến Post (posts, post_images, post_comments, post_likes, post_shares)
-- ================================

-- Xóa các bảng theo thứ tự để tránh lỗi foreign key constraint
-- Xóa các bảng con trước, sau đó mới xóa bảng cha

-- 1. Xóa bảng post_shares (nếu tồn tại)
DROP TABLE IF EXISTS qhomebaseapp.post_shares CASCADE;

-- 2. Xóa bảng post_likes (nếu tồn tại)
DROP TABLE IF EXISTS qhomebaseapp.post_likes CASCADE;

-- 3. Xóa bảng post_comments (nếu tồn tại)
DROP TABLE IF EXISTS qhomebaseapp.post_comments CASCADE;

-- 4. Xóa bảng post_images (nếu tồn tại)
DROP TABLE IF EXISTS qhomebaseapp.post_images CASCADE;

-- 5. Xóa bảng posts (nếu tồn tại)
DROP TABLE IF EXISTS qhomebaseapp.posts CASCADE;

-- Xóa các index nếu còn tồn tại (nếu có lỗi thì bỏ qua)
DROP INDEX IF EXISTS qhomebaseapp.idx_posts_user_id;
DROP INDEX IF EXISTS qhomebaseapp.idx_posts_topic;
DROP INDEX IF EXISTS qhomebaseapp.idx_posts_created_at;
DROP INDEX IF EXISTS qhomebaseapp.idx_post_images_post_id;
DROP INDEX IF EXISTS qhomebaseapp.idx_post_comments_post_id;
DROP INDEX IF EXISTS qhomebaseapp.idx_post_likes_post_id;
DROP INDEX IF EXISTS qhomebaseapp.idx_post_shares_post_id;

