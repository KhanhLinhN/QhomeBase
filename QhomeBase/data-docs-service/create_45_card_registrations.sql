-- Script to create 15 card registrations for each card type
-- Total: 45 registrations (15 Resident Cards + 15 Elevator Cards + 15 Vehicle Registrations)
-- This script can be run multiple times - it will truncate existing test data first

-- Truncate tables to allow re-running the script
-- Note: CASCADE is safe here because invoices only reference these tables via externalRefId (UUID),
-- not through foreign key constraints, so invoices will not be affected
TRUNCATE TABLE card.resident_card_registration CASCADE;
TRUNCATE TABLE card.elevator_card_registration CASCADE;
TRUNCATE TABLE card.register_vehicle CASCADE;

DO $$
DECLARE
    -- Variables for Resident Card
    resident_user_id UUID;
    resident_unit_id UUID;
    resident_resident_id UUID;
    resident_unit_code TEXT;
    resident_building_code TEXT;
    resident_full_name TEXT;
    resident_phone TEXT;
    
    -- Variables for Elevator Card
    elevator_user_id UUID;
    elevator_unit_id UUID;
    elevator_resident_id UUID;
    elevator_unit_code TEXT;
    elevator_building_code TEXT;
    elevator_full_name TEXT;
    elevator_phone TEXT;
    
    -- Variables for Vehicle Registration
    vehicle_user_id UUID;
    vehicle_unit_id UUID;
    vehicle_unit_code TEXT;
    vehicle_building_code TEXT;
    
    -- Counter
    i INTEGER;
BEGIN
    -- ========================================
    -- 1. RESIDENT CARD REGISTRATIONS (15 records)
    -- All in PENDING status with PAID payment_status for testing approval
    -- ========================================
    FOR i IN 1..15 LOOP
        -- Get random resident with unit
        SELECT 
            r.user_id,
            h.unit_id,
            r.id,
            u.code,
            b.code,
            r.full_name,
            r.phone
        INTO 
            resident_user_id,
            resident_unit_id,
            resident_resident_id,
            resident_unit_code,
            resident_building_code,
            resident_full_name,
            resident_phone
        FROM data.residents r
        INNER JOIN data.households h ON h.primary_resident_id = r.id
        INNER JOIN data.units u ON u.id = h.unit_id
        INNER JOIN data.buildings b ON b.id = u.building_id
        WHERE r.user_id IS NOT NULL
          AND r.status = 'ACTIVE'
        ORDER BY RANDOM()
        LIMIT 1;
        
        -- Insert Resident Card Registration with different statuses
        INSERT INTO card.resident_card_registration (
            id,
            user_id,
            unit_id,
            request_type,
            resident_id,
            full_name,
            apartment_number,
            building_name,
            citizen_id,
            phone_number,
            note,
            status,
            payment_status,
            payment_amount,
            payment_date,
            payment_gateway,
            vnpay_transaction_ref,
            admin_note,
            approved_by,
            approved_at,
            rejection_reason,
            created_at,
            updated_at
        ) VALUES (
            gen_random_uuid(),
            resident_user_id,
            resident_unit_id,
            CASE 
                WHEN i <= 5 THEN 'NEW_CARD'
                WHEN i <= 10 THEN 'REPLACE_CARD'
                ELSE 'LOST_CARD'
            END,
            resident_resident_id,
            resident_full_name,
            resident_unit_code,
            resident_building_code,
            (SELECT national_id FROM data.residents WHERE id = resident_resident_id),
            resident_phone,
            CASE 
                WHEN i = 1 THEN 'Thẻ mới cho cư dân mới'
                WHEN i = 2 THEN 'Thay thẻ bị hỏng'
                WHEN i = 3 THEN 'Mất thẻ, cần cấp lại'
                ELSE NULL
            END,
            'PENDING', -- All in PENDING for testing approval (Resident Card uses PENDING)
            'PAID', -- All with PAID payment_status (required for approval)
            30000, -- Payment amount
            NOW() - (i * INTERVAL '1 day'), -- Payment date
            'VNPAY', -- Payment gateway
            'VNPAY_' || gen_random_uuid()::TEXT, -- Transaction ref
            NULL, -- Admin note (will be set when approved)
            NULL, -- Approved by (will be set when approved)
            NULL, -- Approved at (will be set when approved)
            NULL, -- Rejection reason (not rejected)
            NOW() - (i * INTERVAL '3 days'),
            NOW() - (i * INTERVAL '1 day')
        );
    END LOOP;
    
    -- ========================================
    -- 2. ELEVATOR CARD REGISTRATIONS (15 records)
    -- All in PENDING status with PAID payment_status for testing approval
    -- ========================================
    FOR i IN 1..15 LOOP
        -- Get random resident with unit
        SELECT 
            r.user_id,
            h.unit_id,
            r.id,
            u.code,
            b.code,
            r.full_name,
            r.phone
        INTO 
            elevator_user_id,
            elevator_unit_id,
            elevator_resident_id,
            elevator_unit_code,
            elevator_building_code,
            elevator_full_name,
            elevator_phone
        FROM data.residents r
        INNER JOIN data.households h ON h.primary_resident_id = r.id
        INNER JOIN data.units u ON u.id = h.unit_id
        INNER JOIN data.buildings b ON b.id = u.building_id
        WHERE r.user_id IS NOT NULL
          AND r.status = 'ACTIVE'
        ORDER BY RANDOM()
        LIMIT 1;
        
        -- Insert Elevator Card Registration with different statuses
        INSERT INTO card.elevator_card_registration (
            id,
            user_id,
            unit_id,
            request_type,
            resident_id,
            full_name,
            apartment_number,
            building_name,
            citizen_id,
            phone_number,
            note,
            status,
            payment_status,
            payment_amount,
            payment_date,
            payment_gateway,
            vnpay_transaction_ref,
            admin_note,
            approved_by,
            approved_at,
            rejection_reason,
            created_at,
            updated_at
        ) VALUES (
            gen_random_uuid(),
            elevator_user_id,
            elevator_unit_id,
            CASE 
                WHEN i <= 5 THEN 'NEW_CARD'
                WHEN i <= 10 THEN 'REPLACE_CARD'
                ELSE 'LOST_CARD'
            END,
            elevator_resident_id,
            elevator_full_name,
            elevator_unit_code,
            elevator_building_code,
            (SELECT national_id FROM data.residents WHERE id = elevator_resident_id),
            elevator_phone,
            CASE 
                WHEN i = 1 THEN 'Thẻ thang máy mới'
                WHEN i = 2 THEN 'Thay thẻ bị hỏng'
                WHEN i = 3 THEN 'Mất thẻ, cần cấp lại'
                ELSE NULL
            END,
            'PENDING', -- All in PENDING for testing approval (Elevator Card uses PENDING)
            'PAID', -- All with PAID payment_status (required for approval)
            30000, -- Payment amount
            NOW() - (i * INTERVAL '1 day'), -- Payment date
            'VNPAY', -- Payment gateway
            'VNPAY_' || gen_random_uuid()::TEXT, -- Transaction ref
            NULL, -- Admin note (will be set when approved)
            NULL, -- Approved by (will be set when approved)
            NULL, -- Approved at (will be set when approved)
            NULL, -- Rejection reason (not rejected)
            NOW() - (i * INTERVAL '3 days'),
            NOW() - (i * INTERVAL '1 day')
        );
    END LOOP;
    
    -- ========================================
    -- 3. VEHICLE REGISTRATIONS (15 records)
    -- All in PENDING status with PAID payment_status for testing approval
    -- ========================================
    FOR i IN 1..15 LOOP
        -- Get random resident with unit
        SELECT 
            r.user_id,
            h.unit_id,
            u.code,
            b.code
        INTO 
            vehicle_user_id,
            vehicle_unit_id,
            vehicle_unit_code,
            vehicle_building_code
        FROM data.residents r
        INNER JOIN data.households h ON h.primary_resident_id = r.id
        INNER JOIN data.units u ON u.id = h.unit_id
        INNER JOIN data.buildings b ON b.id = u.building_id
        WHERE r.user_id IS NOT NULL
          AND r.status = 'ACTIVE'
        ORDER BY RANDOM()
        LIMIT 1;
        
        -- Insert Vehicle Registration with different statuses
        INSERT INTO card.register_vehicle (
            id,
            user_id,
            unit_id,
            service_type,
            request_type,
            vehicle_type,
            license_plate,
            vehicle_brand,
            vehicle_color,
            apartment_number,
            building_name,
            note,
            status,
            payment_status,
            payment_amount,
            payment_date,
            payment_gateway,
            vnpay_transaction_ref,
            admin_note,
            approved_by,
            approved_at,
            rejection_reason,
            created_at,
            updated_at
        ) VALUES (
            gen_random_uuid(),
            vehicle_user_id,
            vehicle_unit_id,
            'VEHICLE_REGISTRATION', -- Service type must be VEHICLE_REGISTRATION for backend filtering
            CASE 
                WHEN i <= 5 THEN 'NEW_CARD'
                WHEN i <= 10 THEN 'REPLACE_CARD'
                ELSE 'LOST_CARD'
            END,
            CASE 
                WHEN i <= 5 THEN 'Xe máy'
                WHEN i <= 10 THEN 'Ô tô'
                ELSE 'Xe đạp'
            END,
            CASE 
                WHEN i <= 5 THEN '30' || LPAD((i + 10)::TEXT, 6, '0') || LPAD(i::TEXT, 2, '0')
                WHEN i <= 10 THEN '51' || LPAD((i + 5)::TEXT, 6, '0') || LPAD(i::TEXT, 2, '0')
                ELSE '29' || LPAD(i::TEXT, 6, '0') || LPAD(i::TEXT, 2, '0')
            END,
            CASE 
                WHEN i <= 5 THEN 'Honda'
                WHEN i <= 8 THEN 'Yamaha'
                WHEN i <= 10 THEN 'Toyota'
                WHEN i <= 12 THEN 'Honda'
                ELSE 'VinFast'
            END,
            CASE 
                WHEN i <= 5 THEN 'Đỏ'
                WHEN i <= 8 THEN 'Xanh'
                WHEN i <= 10 THEN 'Trắng'
                WHEN i <= 12 THEN 'Đen'
                ELSE 'Bạc'
            END,
            vehicle_unit_code,
            vehicle_building_code,
            CASE 
                WHEN i = 1 THEN 'Đăng ký thẻ xe mới'
                WHEN i = 2 THEN 'Thay thẻ bị hỏng'
                WHEN i = 3 THEN 'Mất thẻ, cần cấp lại'
                ELSE NULL
            END,
            'PENDING', -- All in PENDING for testing approval (Vehicle uses PENDING)
            'PAID', -- All with PAID payment_status (required for approval)
            30000, -- Payment amount
            NOW() - (i * INTERVAL '1 day'), -- Payment date
            'VNPAY', -- Payment gateway
            'VNPAY_' || gen_random_uuid()::TEXT, -- Transaction ref
            NULL, -- Admin note (will be set when approved)
            NULL, -- Approved by (will be set when approved)
            NULL, -- Approved at (will be set when approved)
            NULL, -- Rejection reason (not rejected)
            NOW() - (i * INTERVAL '3 days'),
            NOW() - (i * INTERVAL '1 day')
        );
    END LOOP;
    
    RAISE NOTICE 'Successfully created 45 card registrations:';
    RAISE NOTICE '  - 15 Resident Card registrations';
    RAISE NOTICE '  - 15 Elevator Card registrations';
    RAISE NOTICE '  - 15 Vehicle registrations';
END $$;

-- Verify the created records
SELECT 
    'Resident Card' as card_type,
    status,
    payment_status,
    COUNT(*) as count
FROM card.resident_card_registration
WHERE created_at >= NOW() - INTERVAL '50 days'
GROUP BY status, payment_status
ORDER BY status, payment_status;

SELECT 
    'Elevator Card' as card_type,
    status,
    payment_status,
    COUNT(*) as count
FROM card.elevator_card_registration
WHERE created_at >= NOW() - INTERVAL '50 days'
GROUP BY status, payment_status
ORDER BY status, payment_status;

SELECT 
    'Vehicle Registration' as card_type,
    status,
    payment_status,
    COUNT(*) as count
FROM card.register_vehicle
WHERE created_at >= NOW() - INTERVAL '50 days'
GROUP BY status, payment_status
ORDER BY status, payment_status;

