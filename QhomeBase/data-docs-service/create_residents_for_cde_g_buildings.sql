-- Script to create residents, households, and household_members for units in buildings C, D, E, G
-- Each unit will have 1 primary resident (chủ hộ) and 1 wife (vợ)
-- This script also creates iam.users and assigns RESIDENT role

-- Truncate households table (this will cascade to household_members)
TRUNCATE TABLE data.households CASCADE;

-- Ensure RESIDENT role exists
INSERT INTO iam.roles (role, description)
VALUES ('RESIDENT', 'Resident')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

DO $$
BEGIN

    -- ========================================
    -- BUILDING C - Create residents and households
    -- ========================================
    
    -- C---01: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---01'
        LIMIT 1
    ),
    user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'tranvanc01',
            'tranvanc01@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            ud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM user_data ud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Trần Văn C01',
            '0901000001',
            'tranvanc01@example.com',
            '001001000001',
            '1985-01-15',
            'ACTIVE',
            ud.user_id,
            now(),
            now()
        FROM unit_data
        CROSS JOIN user_data ud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id, user_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '2 years',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- C---01: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---01'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0901000001'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'tranthic01',
            'tranthic01@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Trần Thị C01',
            '0901000006',
            'tranthic01@example.com',
            '001001000006',
            '1987-03-20',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- C---02: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---02'
        LIMIT 1
    ),
    user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'lethic02',
            'lethic02@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            ud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM user_data ud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Lê Thị C02',
            '0901000002',
            'lethic02@example.com',
            '001001000002',
            '1990-03-20',
            'ACTIVE',
            ud.user_id,
            now(),
            now()
        FROM unit_data
        CROSS JOIN user_data ud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id, user_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 year',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- C---02: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---02'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0901000002'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'levanc02',
            'levanc02@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Lê Văn C02',
            '0901000007',
            'levanc02@example.com',
            '001001000007',
            '1992-05-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- C---03: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---03'
        LIMIT 1
    ),
    user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'phamvanc03',
            'phamvanc03@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            ud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM user_data ud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Phạm Văn C03',
            '0901000003',
            'phamvanc03@example.com',
            '001001000003',
            '1988-05-10',
            'ACTIVE',
            ud.user_id,
            now(),
            now()
        FROM unit_data
        CROSS JOIN user_data ud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id, user_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '6 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- C---03: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---03'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0901000003'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'phamthic03',
            'phamthic03@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Phạm Thị C03',
            '0901000008',
            'phamthic03@example.com',
            '001001000008',
            '1990-07-05',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- C---04: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---04'
        LIMIT 1
    ),
    user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'hoangthic04',
            'hoangthic04@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            ud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM user_data ud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Hoàng Thị C04',
            '0901000004',
            'hoangthic04@example.com',
            '001001000004',
            '1992-07-25',
            'ACTIVE',
            ud.user_id,
            now(),
            now()
        FROM unit_data
        CROSS JOIN user_data ud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id, user_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '3 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- C---04: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---04'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0901000004'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'hoangvanc04',
            'hoangvanc04@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Hoàng Văn C04',
            '0901000009',
            'hoangvanc04@example.com',
            '001001000009',
            '1994-09-10',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- C---05: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---05'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Vũ Văn C05',
            '0901000005',
            'vuvanc05@example.com',
            '001001000005',
            '1987-09-30',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 month',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- C---05: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C---05'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0901000005'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'vuthic05',
            'vuthic05@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Vũ Thị C05',
            '0901000010',
            'vuthic05@example.com',
            '001001000010',
            '1989-11-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- ========================================
    -- BUILDING D - Create residents and households
    -- ========================================
    
    -- D---01: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---01'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Đặng Văn D01',
            '0902000001',
            'dangvand01@example.com',
            '001002000001',
            '1986-02-14',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '2 years',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- D---01: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---01'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0902000001'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'dangthid01',
            'dangthid01@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Đặng Thị D01',
            '0902000006',
            'dangthid01@example.com',
            '001002000006',
            '1988-04-10',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- D---02: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---02'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Bùi Thị D02',
            '0902000002',
            'buithid02@example.com',
            '001002000002',
            '1991-04-18',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 year',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- D---02: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---02'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0902000002'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'buivand02',
            'buivand02@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Bùi Văn D02',
            '0902000007',
            'buivand02@example.com',
            '001002000007',
            '1993-06-20',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- D---03: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---03'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Ngô Văn D03',
            '0902000003',
            'ngovand03@example.com',
            '001002000003',
            '1989-06-22',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '6 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- D---03: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---03'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0902000003'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'ngothid03',
            'ngothid03@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Ngô Thị D03',
            '0902000008',
            'ngothid03@example.com',
            '001002000008',
            '1991-08-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- D---04: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---04'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Đỗ Thị D04',
            '0902000004',
            'dothid04@example.com',
            '001002000004',
            '1993-08-12',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '3 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- D---04: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---04'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0902000004'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'dovand04',
            'dovand04@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Đỗ Văn D04',
            '0902000009',
            'dovand04@example.com',
            '001002000009',
            '1995-10-05',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- D---05: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---05'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Lý Văn D05',
            '0902000005',
            'lyvand05@example.com',
            '001002000005',
            '1988-10-25',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 month',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- D---05: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'D' AND u.code = 'D---05'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0902000005'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'lythid05',
            'lythid05@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Lý Thị D05',
            '0902000010',
            'lythid05@example.com',
            '001002000010',
            '1990-12-20',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- ========================================
    -- BUILDING E - Create residents and households
    -- ========================================
    
    -- E---01: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---01'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Võ Văn E01',
            '0903000001',
            'vovane01@example.com',
            '001003000001',
            '1987-01-20',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '2 years',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- E---01: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---01'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0903000001'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'vothie01',
            'vothie01@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Võ Thị E01',
            '0903000006',
            'vothie01@example.com',
            '001003000006',
            '1989-03-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- E---02: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---02'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Hồ Thị E02',
            '0903000002',
            'hothie02@example.com',
            '001003000002',
            '1992-03-15',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 year',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- E---02: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---02'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0903000002'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'hovane02',
            'hovane02@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Hồ Văn E02',
            '0903000007',
            'hovane02@example.com',
            '001003000007',
            '1994-05-10',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- E---03: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---03'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Tôn Văn E03',
            '0903000003',
            'tonvane03@example.com',
            '001003000003',
            '1990-05-10',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '6 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- E---03: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---03'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0903000003'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'tonthie03',
            'tonthie03@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Tôn Thị E03',
            '0903000008',
            'tonthie03@example.com',
            '001003000008',
            '1992-07-05',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- E---04: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---04'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Chu Thị E04',
            '0903000004',
            'chuthie04@example.com',
            '001003000004',
            '1994-07-25',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '3 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- E---04: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---04'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0903000004'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'chuvane04',
            'chuvane04@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Chu Văn E04',
            '0903000009',
            'chuvane04@example.com',
            '001003000009',
            '1996-09-20',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- E---05: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---05'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Dương Văn E05',
            '0903000005',
            'duongvane05@example.com',
            '001003000005',
            '1989-09-30',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 month',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- E---05: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'E' AND u.code = 'E---05'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0903000005'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'duongthie05',
            'duongthie05@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Dương Thị E05',
            '0903000010',
            'duongthie05@example.com',
            '001003000010',
            '1991-11-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- ========================================
    -- BUILDING G - Create residents and households
    -- ========================================
    
    -- G---01: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---01'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Trịnh Văn G01',
            '0904000001',
            'trinhvang01@example.com',
            '001004000001',
            '1986-02-20',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '2 years',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- G---01: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---01'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0904000001'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'trinhthig01',
            'trinhthig01@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Trịnh Thị G01',
            '0904000006',
            'trinhthig01@example.com',
            '001004000006',
            '1988-04-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- G---02: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---02'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Lưu Thị G02',
            '0904000002',
            'luuthig02@example.com',
            '001004000002',
            '1991-04-15',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 year',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- G---02: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---02'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0904000002'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'luuvang02',
            'luuvang02@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Lưu Văn G02',
            '0904000007',
            'luuvang02@example.com',
            '001004000007',
            '1993-06-10',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 year',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- G---03: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---03'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Mai Văn G03',
            '0904000003',
            'maivang03@example.com',
            '001004000003',
            '1988-06-10',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '6 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- G---03: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---03'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0904000003'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'maithig03',
            'maithig03@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Mai Thị G03',
            '0904000008',
            'maithig03@example.com',
            '001004000008',
            '1990-08-05',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '6 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- G---04: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---04'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Tạ Thị G04',
            '0904000004',
            'tathig04@example.com',
            '001004000004',
            '1992-08-12',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '3 months',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- G---04: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---04'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0904000004'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'tavang04',
            'tavang04@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Tạ Văn G04',
            '0904000009',
            'tavang04@example.com',
            '001004000009',
            '1994-10-15',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '3 months',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

    -- G---05: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---05'
        LIMIT 1
    ),
    resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Phan Văn G05',
            '0904000005',
            'phanvang05@example.com',
            '001004000005',
            '1989-10-25',
            'ACTIVE',
            NULL,
            now(),
            now()
        FROM unit_data
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    ),
    household_data AS (
        INSERT INTO data.households (
            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            ud.unit_id,
            'OWNER',
            rd.resident_id,
            CURRENT_DATE - INTERVAL '1 month',
            NULL,
            now(),
            now()
        FROM unit_data ud
        CROSS JOIN resident_data rd
        RETURNING id as household_id, primary_resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hd.household_id,
        hd.primary_resident_id,
        'Chủ hộ',
        true,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_data hd;

    -- G---05: Add wife (vợ) for primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'G' AND u.code = 'G---05'
        LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id, ud.unit_id
        FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (
            SELECT r.id
            FROM data.residents r
            WHERE r.phone = '0904000005'
            LIMIT 1
        )
        LIMIT 1
    ),
    wife_user_data AS (
        INSERT INTO iam.users (
            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'phanthig05',
            'phanthig05@example.com',
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password123
            true,
            NULL,
            0,
            NULL,
            now(),
            now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET
            email = EXCLUDED.email,
            updated_at = now()
        RETURNING id as user_id
    ),
    wife_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            wud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM wife_user_data wud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    wife_resident_data AS (
        INSERT INTO data.residents (
            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at
        )
        SELECT 
            gen_random_uuid(),
            'Phan Thị G05',
            '0904000010',
            'phanthig05@example.com',
            '001004000010',
            '1991-12-20',
            'ACTIVE',
            wud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN wife_user_data wud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name,
            email = EXCLUDED.email,
            national_id = EXCLUDED.national_id,
            dob = EXCLUDED.dob,
            status = EXCLUDED.status,
            user_id = EXCLUDED.user_id,
            updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (
        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at
    )
    SELECT 
        gen_random_uuid(),
        hi.household_id,
        wrd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '1 month',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN wife_resident_data wrd;

END $$;

-- Summary query to verify the created data
SELECT 
    b.code as building_code,
    u.code as unit_code,
    r.full_name as primary_resident_name,
    r.phone,
    r.email,
    h.kind as household_kind,
    h.start_date
FROM data.households h
INNER JOIN data.units u ON h.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN data.residents r ON h.primary_resident_id = r.id
WHERE b.code IN ('C', 'D', 'E', 'G')
ORDER BY b.code, u.code;

