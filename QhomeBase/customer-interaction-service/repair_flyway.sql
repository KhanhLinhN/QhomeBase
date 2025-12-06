-- =====================================================
-- Script SQL để repair Flyway schema history
-- Chạy script này trong PostgreSQL database của customer-interaction-service
-- =====================================================

-- 1. Xóa migration V8 đã bị xóa khỏi source code
-- Lưu ý: Kiểm tra xem có record V8 không trước khi xóa
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '8') THEN
        DELETE FROM flyway_schema_history WHERE version = '8';
        RAISE NOTICE 'Đã xóa migration V8 khỏi schema history';
    ELSE
        RAISE NOTICE 'Migration V8 không tồn tại trong schema history';
    END IF;
END $$;

-- 2. Reset checksum cho tất cả các migration để Flyway tính lại
-- Flyway repair sẽ tự động tính lại checksum khi chạy
UPDATE flyway_schema_history 
SET checksum = NULL
WHERE version IN ('9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28')
AND installed_rank IS NOT NULL;

-- 3. Xem danh sách migrations hiện tại để kiểm tra
SELECT version, description, type, installed_on, success, checksum 
FROM flyway_schema_history 
ORDER BY installed_rank;

-- =====================================================
-- HƯỚNG DẪN:
-- =====================================================
-- 1. Chạy script SQL này trong database
-- 2. Khởi động lại ứng dụng Spring Boot
--    (Đã bật flyway.repair-on-migrate=true trong application.properties)
-- 3. Flyway sẽ tự động repair checksum cho các migration
-- 4. Sau khi repair xong, có thể bật lại validate-on-migrate=true

