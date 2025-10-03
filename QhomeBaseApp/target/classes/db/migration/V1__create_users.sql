-- Tạo schema nếu chưa có
CREATE SCHEMA IF NOT EXISTS qhomebaseapp;

-- Tạo bảng users
CREATE TABLE IF NOT EXISTS qhomebaseapp.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    reset_otp VARCHAR(255),
    otp_expiry TIMESTAMP,
    phone_number VARCHAR(20),
    zalo_id VARCHAR(255)
);
