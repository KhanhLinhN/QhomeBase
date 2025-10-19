-- V18__insert_sample_bills.sql

-- Giả sử có 1 user test với id = 1
INSERT INTO qhomebaseapp.bills (user_id, bill_type, amount, billing_month, status, description, created_at, updated_at)
VALUES
(1, 'ELECTRICITY', 350000, '2025-01-01', 'PAID', 'Tiền điện tháng 1', NOW(), NOW()),
(1, 'WATER', 120000, '2025-01-01', 'PAID', 'Tiền nước tháng 1', NOW(), NOW()),
(1, 'INTERNET', 200000, '2025-01-01', 'PAID', 'Cước internet tháng 1', NOW(), NOW()),
(1, 'ELECTRICITY', 370000, '2025-02-01', 'PAID', 'Tiền điện tháng 2', NOW(), NOW()),
(1, 'WATER', 130000, '2025-02-01', 'PAID', 'Tiền nước tháng 2', NOW(), NOW()),
(1, 'INTERNET', 210000, '2025-02-01', 'PAID', 'Cước internet tháng 2', NOW(), NOW()),
(1, 'ELECTRICITY', 390000, '2025-03-01', 'PAID', 'Tiền điện tháng 3', NOW(), NOW()),
(1, 'WATER', 150000, '2025-03-01', 'PAID', 'Tiền nước tháng 3', NOW(), NOW()),
(1, 'INTERNET', 210000, '2025-03-01', 'PAID', 'Cước internet tháng 3', NOW(), NOW()),
(1, 'ELECTRICITY', 400000, '2025-04-01', 'UNPAID', 'Tiền điện tháng 4', NOW(), NOW()),
(1, 'WATER', 160000, '2025-04-01', 'UNPAID', 'Tiền nước tháng 4', NOW(), NOW()),
(1, 'INTERNET', 220000, '2025-04-01', 'UNPAID', 'Cước internet tháng 4', NOW(), NOW());
