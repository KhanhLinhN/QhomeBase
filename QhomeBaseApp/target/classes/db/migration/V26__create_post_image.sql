-- Tạo bảng post_images trong schema qhomebaseapp
CREATE TABLE IF NOT EXISTS qhomebaseapp.post_images (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    url TEXT NOT NULL,
    CONSTRAINT fk_post
        FOREIGN KEY(post_id)
        REFERENCES qhomebaseapp.posts(id)
        ON DELETE CASCADE
);
