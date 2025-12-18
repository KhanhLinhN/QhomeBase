ALTER TABLE files.contracts ALTER COLUMN status DROP DEFAULT;
DELETE FROM files.contracts WHERE contract_number LIKE 'TEST-%';

-- ========================================
-- RENEWAL SCENARIOS - Logic mới dựa vào số ngày còn lại
-- ========================================

-- 1. PENDING contracts - sẽ nhận reminder lần 1 (endDate trong 28-32 ngày)
-- Logic: daysUntilEndDate >= 28 && daysUntilEndDate <= 32 && renewalReminderSentAt == null
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-PENDING',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '30 days', -- 30 ngày từ hôm nay (nằm trong khoảng 28-32)
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 1;

-- 2. REMINDED contracts - đã nhận reminder lần 1, sẽ nhận reminder lần 2
-- Logic: daysUntilEndDate >= 18 && daysUntilEndDate <= 22 (target: 20 ngày)
-- Điều kiện: renewalReminderSentAt != null && firstReminderDate.isBefore(today)
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-2',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '20 days', -- 20 ngày từ hôm nay (target cho reminder 2)
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'REMINDED', 
    NOW() - INTERVAL '10 days', -- Đã gửi lần 1 cách đây 10 ngày (khi còn 30 ngày)
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 2;

-- 3. REMINDED contracts - đã nhận reminder lần 1, sẽ nhận reminder lần 3 (BẮT BUỘC)
-- Logic: daysUntilEndDate >= 8 && daysUntilEndDate <= 12 (target: 10 ngày)
-- Điều kiện: renewalReminderSentAt != null && firstReminderDate.isBefore(today)
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-3',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '10 days', -- 10 ngày từ hôm nay (target cho reminder 3)
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'REMINDED', 
    NOW() - INTERVAL '20 days', -- Đã gửi lần 1 cách đây 20 ngày (khi còn 30 ngày)
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 3;

-- 4. REMINDED contracts - đã nhận reminder lần 1, 21 ngày trước (sẽ bị đánh dấu DECLINED)
-- Logic: Đã gửi reminder 3 (reminderCount >= 3) và (đã hết hạn HOẶC còn <= 5 ngày và đã qua 20 ngày)
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-21DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '3 days', -- Còn 3 ngày (sẽ bị đánh dấu DECLINED vì còn <= 5 ngày và đã qua 20 ngày từ lần nhắc đầu)
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'REMINDED', 
    NOW() - INTERVAL '21 days', -- Đã gửi lần 1 cách đây 21 ngày (> 20 ngày)
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 4;

-- 5. DECLINED contracts - đã bị đánh dấu DECLINED
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at, renewal_declined_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-DECLINED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '25 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'DECLINED', 
    NOW() - INTERVAL '25 days', 
    NOW() - INTERVAL '5 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 0;

-- ========================================
-- STATUS SCENARIOS
-- ========================================

-- 6. INACTIVE contracts - sẽ được activate tự động khi đến startDate (tomorrow)
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-INACTIVE-1DAY',
    'RENTAL',
    CURRENT_DATE + INTERVAL '1 day',
    CURRENT_DATE + INTERVAL '365 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'INACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 1;

-- 7. INACTIVE contracts - sẽ được activate sau 7 ngày
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-INACTIVE-7DAYS',
    'RENTAL',
    CURRENT_DATE + INTERVAL '7 days',
    CURRENT_DATE + INTERVAL '372 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'INACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 2;

-- 8. EXPIRED contracts - insert trực tiếp với status = EXPIRED
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-EXPIRED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '365 days',
    CURRENT_DATE - INTERVAL '30 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'EXPIRED', 'DECLINED',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 3;

-- 9. CANCELLED contracts - insert trực tiếp với status = CANCELLED
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    checkout_date, monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-CANCELLED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '180 days',
    CURRENT_DATE + INTERVAL '90 days',
    CURRENT_DATE - INTERVAL '5 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'CANCELLED', 'DECLINED',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 4;

-- 10. NO_ENDDATE contracts - không có endDate (KHÔNG bị mark EXPIRED)
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-NO-ENDDATE',
    'RENTAL',
    CURRENT_DATE - INTERVAL '100 days',
    NULL,
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 5;

-- 11. PURCHASE contracts - hợp đồng mua (không có renewal status)
WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    purchase_price, status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-PURCHASE',
    'PURCHASE',
    CURRENT_DATE - INTERVAL '200 days',
    CURRENT_DATE - INTERVAL '10 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 2000000000
        WHEN nu.area_m2 < 80 THEN 3000000000
        WHEN nu.area_m2 < 100 THEN 4000000000
        ELSE 5000000000
    END,
    'ACTIVE',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 6;

-- ========================================
-- KIỂM TRA KẾT QUẢ
-- ========================================

SELECT 
    status,
    COUNT(*) as count
FROM files.contracts
WHERE contract_number LIKE 'TEST-%'
GROUP BY status
ORDER BY status;

SELECT 
    CASE 
        WHEN contract_number LIKE '%INACTIVE%' THEN 'Should be INACTIVE'
        WHEN contract_number LIKE '%EXPIRED%' THEN 'Should be EXPIRED'
        WHEN contract_number LIKE '%CANCELLED%' THEN 'Should be CANCELLED'
        ELSE 'Should be ACTIVE'
    END as expected_status,
    status as actual_status,
    COUNT(*)
FROM files.contracts
WHERE contract_number LIKE 'TEST-%'
GROUP BY expected_status, status
ORDER BY expected_status, status;

-- Kiểm tra các contracts sẽ trigger reminder
SELECT 
    c.contract_number,
    u.code as unit_code,
    b.code as building_code,
    c.status,
    c.renewal_status,
    c.end_date,
    (c.end_date - CURRENT_DATE) as days_until_expiry,
    c.renewal_reminder_sent_at,
    CASE 
        WHEN c.renewal_reminder_sent_at IS NOT NULL 
        THEN EXTRACT(DAY FROM (NOW() - c.renewal_reminder_sent_at))::integer
        ELSE NULL
    END as days_since_reminder,
    CASE 
        WHEN c.contract_type = 'RENTAL' 
             AND c.status = 'ACTIVE' 
             AND c.end_date IS NOT NULL
             AND c.renewal_status = 'PENDING'
             AND c.renewal_reminder_sent_at IS NULL
             AND (c.end_date - CURRENT_DATE) >= 28 
             AND (c.end_date - CURRENT_DATE) <= 32
        THEN 'WILL_TRIGGER_REMINDER_1'
        WHEN c.contract_type = 'RENTAL' 
             AND c.status = 'ACTIVE' 
             AND c.end_date IS NOT NULL
             AND c.renewal_status = 'REMINDED'
             AND c.renewal_reminder_sent_at IS NOT NULL
             AND (c.end_date - CURRENT_DATE) >= 18 
             AND (c.end_date - CURRENT_DATE) <= 22
             AND (c.end_date - CURRENT_DATE) > 0
             AND DATE_TRUNC('day', c.renewal_reminder_sent_at) < CURRENT_DATE
        THEN 'WILL_TRIGGER_REMINDER_2'
        WHEN c.contract_type = 'RENTAL' 
             AND c.status = 'ACTIVE' 
             AND c.end_date IS NOT NULL
             AND c.renewal_status = 'REMINDED'
             AND c.renewal_reminder_sent_at IS NOT NULL
             AND (c.end_date - CURRENT_DATE) >= 8 
             AND (c.end_date - CURRENT_DATE) <= 12
             AND (c.end_date - CURRENT_DATE) > 0
             AND DATE_TRUNC('day', c.renewal_reminder_sent_at) < CURRENT_DATE
        THEN 'WILL_TRIGGER_REMINDER_3'
        WHEN c.contract_type = 'RENTAL' 
             AND c.status = 'ACTIVE' 
             AND c.renewal_status = 'REMINDED'
             AND c.renewal_reminder_sent_at IS NOT NULL
             AND (
                 (c.end_date IS NOT NULL AND c.end_date < CURRENT_DATE)
                 OR (c.end_date IS NOT NULL AND (c.end_date - CURRENT_DATE) <= 5 
                     AND EXTRACT(DAY FROM (NOW() - c.renewal_reminder_sent_at)) >= 20)
             )
        THEN 'WILL_BE_MARKED_DECLINED'
        ELSE 'NO_REMINDER'
    END as reminder_trigger_status
FROM files.contracts c
LEFT JOIN data.units u ON c.unit_id = u.id
LEFT JOIN data.buildings b ON u.building_id = b.id
WHERE c.contract_number LIKE 'TEST-%'
ORDER BY 
    b.code,
    CASE 
        WHEN c.contract_number LIKE 'TEST-RENEWAL-%' THEN 1
        WHEN c.contract_number LIKE 'TEST-STATUS-%' THEN 2
        ELSE 3
    END,
    c.contract_number;
