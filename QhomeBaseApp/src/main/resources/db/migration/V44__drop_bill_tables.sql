-- ================================
-- V44__drop_bill_tables.sql
-- Xóa tất cả các bảng liên quan đến Bill (bills, payments)
-- ================================

-- Xóa các bảng theo thứ tự để tránh lỗi foreign key constraint
-- Xóa các bảng con trước, sau đó mới xóa bảng cha

-- 1. Xóa bảng payments (nếu tồn tại) - có foreign key đến bills
DROP TABLE IF EXISTS qhomebaseapp.payments CASCADE;

-- 2. Xóa bảng bills (nếu tồn tại)
DROP TABLE IF EXISTS qhomebaseapp.bills CASCADE;

-- Xóa các index nếu còn tồn tại
DROP INDEX IF EXISTS qhomebaseapp.idx_bills_user_id;
DROP INDEX IF EXISTS qhomebaseapp.idx_bills_month;

