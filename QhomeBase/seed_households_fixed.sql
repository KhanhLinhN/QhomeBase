-- Fixed script to seed households for buildings C, D, E, G
-- This script ensures pgcrypto extension exists and handles errors gracefully

-- Step 1: Ensure required extension exists
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Step 2: Truncate households (CASCADE will also delete household_members)
TRUNCATE TABLE data.households CASCADE;

-- Step 3: Ensure RESIDENT role exists
INSERT INTO iam.roles (role, description)
VALUES ('RESIDENT', 'Resident')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 4: Verify that buildings and units exist before proceeding
DO $$
DECLARE
    missing_units TEXT;
BEGIN
    -- Check if required units exist
    SELECT string_agg(unit_code, ', ')
    INTO missing_units
    FROM (
        VALUES 
            ('C', 'C1---01'), ('C', 'C1---02'), ('C', 'C2---03'), ('C', 'C2---04'), ('C', 'C3---05'),
            ('D', 'D1---01'), ('D', 'D1---02'), ('D', 'D2---03'), ('D', 'D2---04'), ('D', 'D3---05'),
            ('E', 'E1---01'), ('E', 'E1---02'), ('E', 'E2---03'), ('E', 'E2---04'), ('E', 'E3---05'),
            ('G', 'G1---01'), ('G', 'G1---02'), ('G', 'G2---03'), ('G', 'G2---04'), ('G', 'G3---05')
    ) AS required(building_code, unit_code)
    WHERE NOT EXISTS (
        SELECT 1
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = required.building_code AND u.code = required.unit_code
    );
    
    IF missing_units IS NOT NULL THEN
        RAISE EXCEPTION 'Missing units: %. Please create these units before running this script.', missing_units;
    END IF;
END $$;

-- Step 5: Create households and residents
-- Building C
DO $$
BEGIN
    -- C1---01: Primary resident
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C1---01'
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
END $$;

-- C1---01: Add spouse (vợ/chồng) for primary resident
DO $$
BEGIN
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C1---01'
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
    spouse_user_data AS (
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
    spouse_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT 
            sud.user_id,
            'RESIDENT',
            now(),
            'system'
        FROM spouse_user_data sud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    spouse_resident_data AS (
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
            sud.user_id,
            now(),
            now()
        FROM household_info hi
        CROSS JOIN spouse_user_data sud
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
        srd.resident_id,
        'Vợ',
        false,
        CURRENT_DATE - INTERVAL '2 years',
        NULL,
        now(),
        now()
    FROM household_info hi
    CROSS JOIN spouse_resident_data srd
    ON CONFLICT (household_id, resident_id) DO NOTHING;
END $$;

-- C1---02: Primary resident
DO $$
BEGIN
    WITH unit_data AS (
        SELECT u.id as unit_id, u.code as unit_code
        FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C1---02'
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
            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
            true, NULL, 0, NULL, now(), now()
        FROM unit_data
        ON CONFLICT (username) DO UPDATE SET email = EXCLUDED.email, updated_at = now()
        RETURNING id as user_id
    ),
    user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT ud.user_id, 'RESIDENT', now(), 'system'
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
END $$;

-- C1---02: Add spouse
DO $$
BEGIN
    WITH unit_data AS (
        SELECT u.id as unit_id FROM data.units u
        INNER JOIN data.buildings b ON u.building_id = b.id
        WHERE b.code = 'C' AND u.code = 'C1---02' LIMIT 1
    ),
    household_info AS (
        SELECT h.id as household_id FROM data.households h
        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id
        WHERE h.primary_resident_id = (SELECT r.id FROM data.residents r WHERE r.phone = '0901000002' LIMIT 1)
        LIMIT 1
    ),
    spouse_user_data AS (
        INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at)
        SELECT gen_random_uuid(), 'levanc02', 'levanc02@example.com',
               '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
               true, NULL, 0, NULL, now(), now()
        FROM household_info
        ON CONFLICT (username) DO UPDATE SET email = EXCLUDED.email, updated_at = now()
        RETURNING id as user_id
    ),
    spouse_user_role_data AS (
        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
        SELECT sud.user_id, 'RESIDENT', now(), 'system'
        FROM spouse_user_data sud
        ON CONFLICT (user_id, role) DO NOTHING
        RETURNING user_id
    ),
    spouse_resident_data AS (
        INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
        SELECT gen_random_uuid(), 'Lê Văn C02', '0901000007', 'levanc02@example.com',
               '001001000007', '1992-05-15', 'ACTIVE', sud.user_id, now(), now()
        FROM household_info hi CROSS JOIN spouse_user_data sud
        ON CONFLICT (phone) DO UPDATE SET
            full_name = EXCLUDED.full_name, email = EXCLUDED.email,
            national_id = EXCLUDED.national_id, dob = EXCLUDED.dob,
            status = EXCLUDED.status, user_id = EXCLUDED.user_id, updated_at = now()
        RETURNING id as resident_id
    )
    INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
    SELECT gen_random_uuid(), hi.household_id, srd.resident_id, 'Chồng', false,
           CURRENT_DATE - INTERVAL '1 year', NULL, now(), now()
    FROM household_info hi CROSS JOIN spouse_resident_data srd
    ON CONFLICT (household_id, resident_id) DO NOTHING;
END $$;

-- Summary query to verify the created data
SELECT 
    b.code as building_code,
    u.code as unit_code,
    r.full_name as primary_resident_name,
    r.phone,
    r.email,
    h.kind as household_kind,
    h.start_date,
    (SELECT COUNT(*) FROM data.household_members hm WHERE hm.household_id = h.id) as member_count
FROM data.households h
INNER JOIN data.units u ON h.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN data.residents r ON h.primary_resident_id = r.id
WHERE b.code IN ('C', 'D', 'E', 'G')
ORDER BY b.code, u.code;





