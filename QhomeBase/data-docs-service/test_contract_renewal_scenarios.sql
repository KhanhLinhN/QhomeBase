TRUNCATE TABLE files.contracts CASCADE;

-- Test contract renewal scenarios for all buildings
-- Sử dụng các unit từ tất cả các tòa nhà (trừ building A)
-- Phân bổ các test scenarios cho các buildings khác nhau

-- Kiểm tra số lượng units có sẵn trước khi insert
DO $$
DECLARE
    unit_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unit_count
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE';
    
    IF unit_count = 0 THEN
        RAISE EXCEPTION 'Không có unit nào thỏa mãn điều kiện (building != A, status = ACTIVE)';
    END IF;
    
    RAISE NOTICE 'Tìm thấy % units thỏa mãn điều kiện', unit_count;
END $$;

-- ========================================
-- RENEWAL SCENARIOS
-- ========================================

-- 1. PENDING contracts - sẽ nhận reminder lần 1 (endDate trong 15-30 ngày)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-PENDING',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '15 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE',
    'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 1
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 2. REMINDED contracts - đã nhận reminder lần 1, 7 ngày trước (sẽ nhận reminder lần 2)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-7DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '19 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE',
    'REMINDED',
    NOW() - INTERVAL '7 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 2
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    renewal_reminder_sent_at = EXCLUDED.renewal_reminder_sent_at,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 3. REMINDED contracts - đã nhận reminder lần 1, 20 ngày trước (sẽ nhận reminder lần 3)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-20DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '1 year',
    CASE 
        WHEN EXTRACT(DAY FROM CURRENT_DATE) < 20 THEN 
            DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '19 days'
        ELSE 
            DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '19 days'
    END,
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE',
    'REMINDED',
    NOW() - INTERVAL '20 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 3
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    renewal_reminder_sent_at = EXCLUDED.renewal_reminder_sent_at,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 4. REMINDED contracts - đã nhận reminder lần 1, 21 ngày trước (sẽ bị đánh dấu DECLINED)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-21DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '1 year',
    CURRENT_DATE + INTERVAL '21 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE',
    'REMINDED',
    NOW() - INTERVAL '21 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 4
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    renewal_reminder_sent_at = EXCLUDED.renewal_reminder_sent_at,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 5. DECLINED contracts - đã bị đánh dấu DECLINED
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at, renewal_declined_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-DECLINED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '1 year',
    CURRENT_DATE + INTERVAL '25 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE',
    'DECLINED',
    NOW() - INTERVAL '25 days',
    NOW() - INTERVAL '5 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 0
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    renewal_reminder_sent_at = EXCLUDED.renewal_reminder_sent_at,
    renewal_declined_at = EXCLUDED.renewal_declined_at,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- ========================================
-- STATUS SCENARIOS
-- ========================================

-- 6. INACTIVE contracts - sẽ được activate tự động khi đến startDate (tomorrow)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
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
    'INACTIVE',
    'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 1 AND nu.rn > 1
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 7. INACTIVE contracts - sẽ được activate sau 7 ngày
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
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
    'INACTIVE',
    'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 2 AND nu.rn > 2
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 8. EXPIRED contracts - insert trực tiếp với status = EXPIRED
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
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
    'EXPIRED',
    'DECLINED',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 3 AND nu.rn > 3
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- Kiểm tra số lượng EXPIRED contracts đã được insert
DO $$
DECLARE
    expired_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO expired_count
    FROM files.contracts
    WHERE contract_number LIKE 'TEST-STATUS-%-EXPIRED' AND status = 'EXPIRED';
    
    IF expired_count = 0 THEN
        RAISE WARNING 'Không có EXPIRED contract nào được insert. Kiểm tra lại điều kiện WHERE nu.rn % 5 = 3 AND nu.rn > 3';
    ELSE
        RAISE NOTICE 'Đã insert % EXPIRED contracts', expired_count;
    END IF;
END $$;

-- 9. CANCELLED contracts - insert trực tiếp với status = CANCELLED
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    checkout_date, monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-CANCELLED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '180 days',
    CURRENT_DATE + INTERVAL '3 months',
    CURRENT_DATE - INTERVAL '5 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'CANCELLED',
    'DECLINED',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 4 AND nu.rn > 4
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    checkout_date = EXCLUDED.checkout_date,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- Kiểm tra số lượng CANCELLED contracts đã được insert
DO $$
DECLARE
    cancelled_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO cancelled_count
    FROM files.contracts
    WHERE contract_number LIKE 'TEST-STATUS-%-CANCELLED' AND status = 'CANCELLED';
    
    IF cancelled_count = 0 THEN
        RAISE WARNING 'Không có CANCELLED contract nào được insert. Kiểm tra lại điều kiện WHERE nu.rn % 5 = 4 AND nu.rn > 4';
    ELSE
        RAISE NOTICE 'Đã insert % CANCELLED contracts', cancelled_count;
    END IF;
END $$;

-- 10. NO_ENDDATE contracts - không có endDate (KHÔNG bị mark EXPIRED)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
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
    'ACTIVE',
    'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 0 AND nu.rn > 5
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    renewal_status = EXCLUDED.renewal_status,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();

-- 11. PURCHASE contracts - hợp đồng mua (không có renewal status)
WITH numbered_units AS (
    SELECT 
        u.id,
        u.code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    purchase_price, status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    nu.id,
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
    NOW(),
    NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 1 AND nu.rn > 6
ON CONFLICT (contract_number) DO UPDATE SET
    status = EXCLUDED.status,
    contract_type = EXCLUDED.contract_type,
    purchase_price = EXCLUDED.purchase_price,
    end_date = EXCLUDED.end_date,
    updated_at = NOW();


-- ========================================
-- KIỂM TRA KẾT QUẢ
-- ========================================

-- Kiểm tra tất cả test contracts (renewal + status)
SELECT 
    c.contract_number,
    u.code as unit_code,
    b.code as building_code,
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
