-- Query để thêm các hợp đồng test ở các giai đoạn khác nhau
-- Sử dụng các unit từ bảng data.units

-- Giả sử hôm nay là 2025-12-06
-- Tạo các hợp đồng với các trạng thái renewal khác nhau:

-- Lưu ý: Cần có ít nhất 1 user trong hệ thống, nếu không có thì dùng UUID tĩnh
-- Hoặc có thể bỏ qua created_by nếu nullable (kiểm tra schema)

-- 1. Hợp đồng sẽ nhận reminder lần 1 (PENDING, endDate trong 30 ngày)
-- EndDate: 15 ngày sau (2025-12-21) - sẽ được gửi reminder lần 1 ngay
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    (SELECT id FROM data.units WHERE code = 'B3---01' LIMIT 1),
    'TEST-RENEWAL-001-PENDING',
    'RENTAL',
    '2024-12-01',
    '2025-12-21', -- 15 ngày sau (sẽ nhận reminder lần 1)
    5000000.00,
    'ACTIVE',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-RENEWAL-001-PENDING');

-- 2. Hợp đồng đã nhận reminder lần 1, 7 ngày trước (sẽ nhận reminder lần 2)
-- Reminder sent: 7 ngày trước
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    (SELECT id FROM data.units WHERE code = 'C5---02' LIMIT 1),
    'TEST-RENEWAL-002-REMINDED-7DAYS',
    'RENTAL',
    '2024-12-01',
    '2025-12-25', -- 19 ngày sau
    6000000.00,
    'ACTIVE',
    'REMINDED',
    NOW() - INTERVAL '7 days', -- 7 ngày trước
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-RENEWAL-002-REMINDED-7DAYS');

-- 3. Hợp đồng đã nhận reminder lần 1, 20 ngày trước (sẽ nhận reminder lần 3 - deadline)
-- Reminder sent: 20 ngày trước
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    (SELECT id FROM data.units WHERE code = 'C5---01' LIMIT 1),
    'TEST-RENEWAL-003-REMINDED-20DAYS',
    'RENTAL',
    '2024-11-01',
    '2025-12-26', -- 20 ngày sau
    7000000.00,
    'ACTIVE',
    'REMINDED',
    NOW() - INTERVAL '20 days', -- 20 ngày trước (sẽ nhận reminder lần 3)
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-RENEWAL-003-REMINDED-20DAYS');

-- 4. Hợp đồng đã nhận reminder lần 1, 21 ngày trước (sẽ bị đánh dấu DECLINED)
-- Reminder sent: 21 ngày trước
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    (SELECT id FROM data.units WHERE code = 'B5---01' LIMIT 1),
    'TEST-RENEWAL-004-REMINDED-21DAYS',
    'RENTAL',
    '2024-11-01',
    '2025-12-27', -- 21 ngày sau
    8000000.00,
    'ACTIVE',
    'REMINDED',
    NOW() - INTERVAL '21 days', -- 21 ngày trước (sẽ bị đánh dấu DECLINED)
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-RENEWAL-004-REMINDED-21DAYS');

-- 5. Hợp đồng đã bị đánh dấu DECLINED (để test hiển thị)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status, renewal_reminder_sent_at, renewal_declined_at,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    (SELECT id FROM data.units WHERE code = 'B1---01' LIMIT 1),
    'TEST-RENEWAL-005-DECLINED',
    'RENTAL',
    '2024-10-01',
    '2025-12-28',
    9000000.00,
    'ACTIVE',
    'DECLINED',
    NOW() - INTERVAL '25 days', -- 25 ngày trước
    NOW() - INTERVAL '5 days', -- Declined 5 ngày trước
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-RENEWAL-005-DECLINED');

-- 6. Hợp đồng sẽ nhận reminder lần 1 trong tương lai (endDate đúng 30 ngày sau)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    (SELECT id FROM data.units WHERE code LIKE 'A2---%' LIMIT 1),
    'TEST-RENEWAL-006-PENDING-30DAYS',
    'RENTAL',
    '2024-12-01',
    CURRENT_DATE + INTERVAL '30 days', -- Đúng 30 ngày sau
    5500000.00,
    'ACTIVE',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-RENEWAL-006-PENDING-30DAYS');

-- ========================================
-- TEST CONTRACT STATUS CHANGES
-- ========================================

-- 7. Hợp đồng INACTIVE - sẽ được activate tự động khi đến startDate (tomorrow)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'A1---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'B3---01' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-001-INACTIVE',
    'RENTAL',
    CURRENT_DATE + INTERVAL '1 day', -- Ngày mai (sẽ được activate)
    CURRENT_DATE + INTERVAL '365 days',
    4000000.00,
    'INACTIVE',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-001-INACTIVE');

-- 8. Hợp đồng INACTIVE - sẽ được activate sau 7 ngày
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'A3---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'C5---02' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-002-INACTIVE-7DAYS',
    'RENTAL',
    CURRENT_DATE + INTERVAL '7 days', -- 7 ngày sau
    CURRENT_DATE + INTERVAL '372 days',
    4500000.00,
    'INACTIVE',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-002-INACTIVE-7DAYS');

-- 9. Hợp đồng ACTIVE - sẽ bị mark EXPIRED (endDate đã qua 1 ngày)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'B2---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'C5---01' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-003-EXPIRED-1DAY',
    'RENTAL',
    CURRENT_DATE - INTERVAL '365 days',
    CURRENT_DATE - INTERVAL '1 day', -- Đã qua 1 ngày (sẽ bị mark EXPIRED)
    5000000.00,
    'ACTIVE',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-003-EXPIRED-1DAY');

-- 10. Hợp đồng ACTIVE - sẽ bị mark EXPIRED (endDate đã qua 30 ngày)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'B4---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'B5---01' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-004-EXPIRED-30DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '395 days',
    CURRENT_DATE - INTERVAL '30 days', -- Đã qua 30 ngày (sẽ bị mark EXPIRED)
    5500000.00,
    'ACTIVE',
    'DECLINED',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-004-EXPIRED-30DAYS');

-- 11. Hợp đồng ACTIVE nhưng không có endDate - KHÔNG bị mark EXPIRED
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'C1---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'B1---01' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-005-NO-ENDDATE',
    'RENTAL',
    CURRENT_DATE - INTERVAL '100 days',
    NULL, -- Không có endDate (sẽ KHÔNG bị mark EXPIRED)
    6000000.00,
    'ACTIVE',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-005-NO-ENDDATE');

-- 12. Hợp đồng PURCHASE (không có renewal status) - sẽ bị mark EXPIRED
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    purchase_price, status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'C2---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code LIKE 'A2---%' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-006-PURCHASE-EXPIRED',
    'PURCHASE',
    CURRENT_DATE - INTERVAL '200 days',
    CURRENT_DATE - INTERVAL '10 days', -- Đã qua 10 ngày (sẽ bị mark EXPIRED)
    2000000000.00,
    'ACTIVE',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-006-PURCHASE-EXPIRED');

-- 13. Hợp đồng đã CANCELLED - KHÔNG bị mark EXPIRED (dù có endDate đã qua)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'C3---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'B3---01' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-007-CANCELLED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '180 days',
    CURRENT_DATE - INTERVAL '5 days', -- Đã qua 5 ngày nhưng CANCELLED (KHÔNG bị mark EXPIRED)
    6500000.00,
    'CANCELLED',
    'PENDING',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-007-CANCELLED');

-- 14. Hợp đồng đã EXPIRED (để test không bị mark lại)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date, 
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
) 
SELECT 
    gen_random_uuid(),
    COALESCE(
        (SELECT id FROM data.units WHERE code LIKE 'C4---%' LIMIT 1),
        (SELECT id FROM data.units WHERE code = 'C5---01' LIMIT 1),
        (SELECT id FROM data.units LIMIT 1)
    ),
    'TEST-STATUS-008-ALREADY-EXPIRED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '400 days',
    CURRENT_DATE - INTERVAL '50 days', -- Đã qua 50 ngày và đã EXPIRED
    7000000.00,
    'EXPIRED', -- Đã expired rồi (không bị mark lại)
    'DECLINED',
    COALESCE((SELECT id FROM iam.users LIMIT 1), '00000000-0000-0000-0000-000000000001'::uuid),
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM files.contracts WHERE contract_number = 'TEST-STATUS-008-ALREADY-EXPIRED');

-- ========================================
-- KIỂM TRA KẾT QUẢ
-- ========================================

-- Kiểm tra tất cả test contracts (renewal + status)
SELECT 
    c.contract_number,
    u.code as unit_code,
    c.contract_type,
    c.status,
    c.start_date,
    c.end_date,
    c.renewal_status,
    c.renewal_reminder_sent_at,
    c.renewal_declined_at,
    CASE 
        WHEN c.renewal_reminder_sent_at IS NOT NULL 
        THEN EXTRACT(DAY FROM (NOW() - c.renewal_reminder_sent_at))::integer
        ELSE NULL
    END as days_since_reminder,
    CASE 
        WHEN c.end_date IS NOT NULL 
        THEN (c.end_date - CURRENT_DATE)
        ELSE NULL
    END as days_until_expiry,
    CASE 
        WHEN c.status = 'INACTIVE' AND c.start_date > CURRENT_DATE 
        THEN (c.start_date - CURRENT_DATE)
        ELSE NULL
    END as days_until_activation,
    CASE 
        WHEN c.status = 'ACTIVE' AND c.end_date IS NOT NULL AND c.end_date < CURRENT_DATE 
        THEN 'SHOULD_BE_EXPIRED'
        WHEN c.status = 'EXPIRED' 
        THEN 'ALREADY_EXPIRED'
        WHEN c.status = 'ACTIVE' AND c.end_date IS NULL 
        THEN 'NO_ENDDATE'
        ELSE 'OK'
    END as expiration_status
FROM files.contracts c
LEFT JOIN data.units u ON c.unit_id = u.id
WHERE c.contract_number LIKE 'TEST-%'
ORDER BY 
    CASE 
        WHEN c.contract_number LIKE 'TEST-RENEWAL-%' THEN 1
        WHEN c.contract_number LIKE 'TEST-STATUS-%' THEN 2
        ELSE 3
    END,
    c.contract_number;

