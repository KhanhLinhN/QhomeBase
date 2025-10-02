-- Xóa dữ liệu cũ trong bảng users
TRUNCATE TABLE login.users RESTART IDENTITY CASCADE;

-- Thêm user mẫu để test OTP
INSERT INTO login.users (username, password, email)
VALUES
  ('tri01', 'pass123', 'minhtrihoangngoc@gmail.com'),
  ('tri02', 'pass456', 'tri02@gmail.com'),
  ('tri03', 'pass789', 'tri03@gmail.com');
