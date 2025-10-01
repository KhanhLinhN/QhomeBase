-- 1. Xóa toàn bộ dữ liệu trong bảng users và reset lại IDENTITY
TRUNCATE TABLE login.users RESTART IDENTITY CASCADE;

-- 2. Bỏ cột 'role' nếu tồn tại
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'login' AND table_name = 'users' AND column_name = 'role'
    ) THEN
        ALTER TABLE login.users DROP COLUMN role;
    END IF;
END $$;

-- 3. Chèn dữ liệu mẫu mới
INSERT INTO login.users (username, password, email)
VALUES
  ('tri01', 'pass123', 'tri01@gmail.com'),
  ('tri02', 'pass456', 'tri02@gmail.com'),
  ('tri03', 'pass789', 'tri03@gmail.com');