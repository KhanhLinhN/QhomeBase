-- Thêm cột reset_otp (mã OTP) và otp_expiry (thời gian hết hạn)
ALTER TABLE login.users
ADD COLUMN IF NOT EXISTS reset_otp VARCHAR(10),
ADD COLUMN IF NOT EXISTS otp_expiry TIMESTAMP;
