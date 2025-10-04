-- V5: Insert categories (chạy 1 lần)
INSERT INTO news_category (code, name) VALUES
('BQT', 'Thông báo từ Ban Quản Trị'),
('BQL', 'Thông báo từ Ban Quản Lý'),
('INTERNAL', 'Thông tin nội bộ'),
('GOV', 'Thông tin từ chính quyền')
ON CONFLICT (code) DO NOTHING; -- nếu là PostgreSQL

-- V5: Insert news
INSERT INTO news
(category_id, title, summary, content, author, source, published_at, pinned, visible_to_all, created_by, created_at)
VALUES
(1, 'Thông báo bảo trì nước', 'Bảo trì hệ thống nước ngày mai',
 'Ngày mai hệ thống nước sẽ bảo trì từ 8h sáng đến 12h trưa. Cư dân chú ý chuẩn bị.',
 'Ban Quản Trị', 'Hệ thống', CURRENT_TIMESTAMP, false, true, 'admin', CURRENT_TIMESTAMP),

(2, 'Họp cư dân tháng 10', 'Hội nghị cư dân tháng 10',
 'Hội nghị cư dân sẽ được tổ chức vào thứ 7 tuần này tại sảnh lớn.',
 'Ban Quản Lý', 'Hệ thống', CURRENT_TIMESTAMP, true, true, 'admin', CURRENT_TIMESTAMP),

(4, 'Thông tin chính quyền', 'Thông báo mới từ UBND phường',
 'Thông báo mới từ UBND phường về các quy định mới',
 'Chính quyền', 'Hệ thống', CURRENT_TIMESTAMP, false, true, 'admin', CURRENT_TIMESTAMP);
