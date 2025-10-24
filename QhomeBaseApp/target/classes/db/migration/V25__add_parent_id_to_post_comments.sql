-- V25__add_parent_id_to_post_comments.sql
-- Thêm cột parent_id để hỗ trợ reply comment (bình luận lồng nhau)

ALTER TABLE qhomebaseapp.post_comments
ADD COLUMN parent_id BIGINT NULL;

ALTER TABLE qhomebaseapp.post_comments
ADD CONSTRAINT fk_post_comments_parent
    FOREIGN KEY (parent_id)
    REFERENCES qhomebaseapp.post_comments (id)
    ON DELETE CASCADE;
