#!/usr/bin/env python3
"""
Script to generate SQL for seeding households for buildings C, D, E, G
This generates the complete SQL script with all units
"""

# Configuration: Building code -> List of unit codes (format: {building}{floor}---{unit})
UNITS = {
    'C': ['C1---01', 'C1---02', 'C2---03', 'C2---04', 'C3---05'],
    'D': ['D1---01', 'D1---02', 'D2---03', 'D2---04', 'D3---05'],
    'E': ['E1---01', 'E1---02', 'E2---03', 'E2---04', 'E3---05'],
    'G': ['G1---01', 'G1---02', 'G2---03', 'G2---04', 'G3---05'],
}

# Resident data template
# Format: (primary_name, primary_phone, primary_email, primary_national_id, primary_dob, 
#          spouse_name, spouse_phone, spouse_email, spouse_national_id, spouse_dob, start_date_offset)
RESIDENT_DATA = {
    'C---01': ('Trần Văn C01', '0901000001', 'tranvanc01@example.com', '001001000001', '1985-01-15',
               'Trần Thị C01', '0901000006', 'tranthic01@example.com', '001001000006', '1987-03-20', '2 years'),
    'C---02': ('Lê Thị C02', '0901000002', 'lethic02@example.com', '001001000002', '1990-03-20',
               'Lê Văn C02', '0901000007', 'levanc02@example.com', '001001000007', '1992-05-15', '1 year'),
    'C---03': ('Phạm Văn C03', '0901000003', 'phamvanc03@example.com', '001001000003', '1988-05-10',
               'Phạm Thị C03', '0901000008', 'phamthic03@example.com', '001001000008', '1990-07-05', '6 months'),
    'C---04': ('Hoàng Thị C04', '0901000004', 'hoangthic04@example.com', '001001000004', '1992-07-25',
               'Hoàng Văn C04', '0901000009', 'hoangvanc04@example.com', '001001000009', '1994-09-10', '3 months'),
    'C---05': ('Vũ Văn C05', '0901000005', 'vuvanc05@example.com', '001001000005', '1987-09-30',
               'Vũ Thị C05', '0901000010', 'vuthic05@example.com', '001001000010', '1989-11-15', '1 month'),
    'D---01': ('Đặng Văn D01', '0902000001', 'dangvand01@example.com', '001002000001', '1986-02-14',
               'Đặng Thị D01', '0902000006', 'dangthid01@example.com', '001002000006', '1988-04-10', '2 years'),
    'D---02': ('Bùi Thị D02', '0902000002', 'buithid02@example.com', '001002000002', '1991-04-18',
               'Bùi Văn D02', '0902000007', 'buivand02@example.com', '001002000007', '1993-06-20', '1 year'),
    'D---03': ('Ngô Văn D03', '0902000003', 'ngovand03@example.com', '001002000003', '1989-06-22',
               'Ngô Thị D03', '0902000008', 'ngothid03@example.com', '001002000008', '1991-08-15', '6 months'),
    'D---04': ('Đỗ Thị D04', '0902000004', 'dothid04@example.com', '001002000004', '1993-08-12',
               'Đỗ Văn D04', '0902000009', 'dovand04@example.com', '001002000009', '1995-10-05', '3 months'),
    'D---05': ('Lý Văn D05', '0902000005', 'lyvand05@example.com', '001002000005', '1988-10-25',
               'Lý Thị D05', '0902000010', 'lythid05@example.com', '001002000010', '1990-12-20', '1 month'),
    'E---01': ('Võ Văn E01', '0903000001', 'vovane01@example.com', '001003000001', '1987-01-20',
               'Võ Thị E01', '0903000006', 'vothie01@example.com', '001003000006', '1989-03-15', '2 years'),
    'E---02': ('Hồ Thị E02', '0903000002', 'hothie02@example.com', '001003000002', '1992-03-15',
               'Hồ Văn E02', '0903000007', 'hovane02@example.com', '001003000007', '1994-05-10', '1 year'),
    'E---03': ('Tôn Văn E03', '0903000003', 'tonvane03@example.com', '001003000003', '1990-05-10',
               'Tôn Thị E03', '0903000008', 'tonthie03@example.com', '001003000008', '1992-07-05', '6 months'),
    'E---04': ('Chu Thị E04', '0903000004', 'chuthie04@example.com', '001003000004', '1994-07-25',
               'Chu Văn E04', '0903000009', 'chuvane04@example.com', '001003000009', '1996-09-20', '3 months'),
    'E---05': ('Dương Văn E05', '0903000005', 'duongvane05@example.com', '001003000005', '1989-09-30',
               'Dương Thị E05', '0903000010', 'duongthie05@example.com', '001003000010', '1991-11-15', '1 month'),
    'G---01': ('Trịnh Văn G01', '0904000001', 'trinhvang01@example.com', '001004000001', '1986-02-20',
               'Trịnh Thị G01', '0904000006', 'trinhthig01@example.com', '001004000006', '1988-04-15', '2 years'),
    'G---02': ('Lưu Thị G02', '0904000002', 'luuthig02@example.com', '001004000002', '1991-04-15',
               'Lưu Văn G02', '0904000007', 'luuvang02@example.com', '001004000007', '1993-06-10', '1 year'),
    'G---03': ('Mai Văn G03', '0904000003', 'maivang03@example.com', '001004000003', '1988-06-10',
               'Mai Thị G03', '0904000008', 'maithig03@example.com', '001004000008', '1990-08-05', '6 months'),
    'G---04': ('Tạ Thị G04', '0904000004', 'tathig04@example.com', '001004000004', '1992-08-12',
               'Tạ Văn G04', '0904000009', 'tavang04@example.com', '001004000009', '1994-10-15', '3 months'),
    'G---05': ('Phan Văn G05', '0904000005', 'phanvang05@example.com', '001004000005', '1989-10-25',
               'Phan Thị G05', '0904000010', 'phanthig05@example.com', '001004000010', '1991-12-20', '1 month'),
}

def generate_username(name, unit_code):
    """Generate username from name and unit code"""
    # Remove Vietnamese diacritics and spaces, convert to lowercase
    name_clean = name.lower().replace(' ', '').replace('ă', 'a').replace('â', 'a').replace('á', 'a').replace('à', 'a').replace('ả', 'a').replace('ã', 'a').replace('ạ', 'a')
    name_clean = name_clean.replace('ê', 'e').replace('é', 'e').replace('è', 'e').replace('ẻ', 'e').replace('ẽ', 'e').replace('ệ', 'e')
    name_clean = name_clean.replace('ô', 'o').replace('ơ', 'o').replace('ó', 'o').replace('ò', 'o').replace('ỏ', 'o').replace('õ', 'o').replace('ọ', 'o')
    name_clean = name_clean.replace('ư', 'u').replace('ú', 'u').replace('ù', 'u').replace('ủ', 'u').replace('ũ', 'u').replace('ụ', 'u')
    name_clean = name_clean.replace('í', 'i').replace('ì', 'i').replace('ỉ', 'i').replace('ĩ', 'i').replace('ị', 'i')
    name_clean = name_clean.replace('ý', 'y').replace('ỳ', 'y').replace('ỷ', 'y').replace('ỹ', 'y').replace('ỵ', 'y')
    name_clean = name_clean.replace('đ', 'd')
    return name_clean + unit_code.lower().replace('---', '')

def determine_relation(primary_name, spouse_name):
    """Determine relation based on names - if primary has 'Thị' (female), spouse is 'Chồng', else 'Vợ'"""
    if 'Thị' in primary_name:
        return 'Chồng'  # Husband
    else:
        return 'Vợ'  # Wife

def generate_sql():
    """Generate the complete SQL script"""
    
    sql = """-- Generated household seed script for buildings C, D, E, G
-- This script creates households with primary residents and their spouses

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
    SELECT string_agg(unit_code, ', ')
    INTO missing_units
    FROM (
        VALUES 
"""
    
    # Add all unit codes to the check
    all_units = []
    for building_code, unit_codes in UNITS.items():
        for unit_code in unit_codes:
            all_units.append(f"            ('{building_code}', '{unit_code}')")
    
    sql += ',\n'.join(all_units)
    sql += """
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

"""
    
    # Generate SQL for each unit
    for building_code, unit_codes in UNITS.items():
        sql += f"\n-- ========================================\n"
        sql += f"-- BUILDING {building_code} - Create residents and households\n"
        sql += f"-- ========================================\n\n"
        
        for unit_code in unit_codes:
            if unit_code not in RESIDENT_DATA:
                continue
                
            data = RESIDENT_DATA[unit_code]
            (p_name, p_phone, p_email, p_nid, p_dob,
             s_name, s_phone, s_email, s_nid, s_dob, date_offset) = data
            
            p_username = generate_username(p_name, unit_code)
            s_username = generate_username(s_name, unit_code)
            relation = determine_relation(p_name, s_name)
            
            # Primary resident block
            sql += f"-- {unit_code}: Primary resident\n"
            sql += f"DO $$\nBEGIN\n"
            sql += f"    WITH unit_data AS (\n"
            sql += f"        SELECT u.id as unit_id, u.code as unit_code\n"
            sql += f"        FROM data.units u\n"
            sql += f"        INNER JOIN data.buildings b ON u.building_id = b.id\n"
            sql += f"        WHERE b.code = '{building_code}' AND u.code = '{unit_code}'\n"
            sql += f"        LIMIT 1\n"
            sql += f"    )\n"
            
            # Check if primary has user (some don't based on original script)
            has_primary_user = unit_code not in ['C---05', 'D---01', 'D---02', 'D---03', 'D---04', 'D---05', 
                                                   'E---01', 'E---02', 'E---03', 'E---04', 'E---05',
                                                   'G---01', 'G---02', 'G---03', 'G---04', 'G---05']
            
            if has_primary_user:
                sql += f"    , user_data AS (\n"
                sql += f"        INSERT INTO iam.users (\n"
                sql += f"            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at\n"
                sql += f"        )\n"
                sql += f"        SELECT \n"
                sql += f"            gen_random_uuid(),\n"
                sql += f"            '{p_username}',\n"
                sql += f"            '{p_email}',\n"
                sql += f"            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',\n"
                sql += f"            true, NULL, 0, NULL, now(), now()\n"
                sql += f"        FROM unit_data\n"
                sql += f"        ON CONFLICT (username) DO UPDATE SET\n"
                sql += f"            email = EXCLUDED.email,\n"
                sql += f"            updated_at = now()\n"
                sql += f"        RETURNING id as user_id\n"
                sql += f"    )\n"
                sql += f"    , user_role_data AS (\n"
                sql += f"        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)\n"
                sql += f"        SELECT ud.user_id, 'RESIDENT', now(), 'system'\n"
                sql += f"        FROM user_data ud\n"
                sql += f"        ON CONFLICT (user_id, role) DO NOTHING\n"
                sql += f"        RETURNING user_id\n"
                sql += f"    )\n"
                sql += f"    , resident_data AS (\n"
                sql += f"        INSERT INTO data.residents (\n"
                sql += f"            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at\n"
                sql += f"        )\n"
                sql += f"        SELECT \n"
                sql += f"            gen_random_uuid(),\n"
                sql += f"            '{p_name}',\n"
                sql += f"            '{p_phone}',\n"
                sql += f"            '{p_email}',\n"
                sql += f"            '{p_nid}',\n"
                sql += f"            '{p_dob}',\n"
                sql += f"            'ACTIVE',\n"
                sql += f"            ud.user_id,\n"
                sql += f"            now(),\n"
                sql += f"            now()\n"
                sql += f"        FROM unit_data\n"
                sql += f"        CROSS JOIN user_data ud\n"
                sql += f"        ON CONFLICT (phone) DO UPDATE SET\n"
                sql += f"            full_name = EXCLUDED.full_name,\n"
                sql += f"            email = EXCLUDED.email,\n"
                sql += f"            national_id = EXCLUDED.national_id,\n"
                sql += f"            dob = EXCLUDED.dob,\n"
                sql += f"            status = EXCLUDED.status,\n"
                sql += f"            user_id = EXCLUDED.user_id,\n"
                sql += f"            updated_at = now()\n"
                sql += f"        RETURNING id as resident_id, user_id\n"
                sql += f"    )\n"
            else:
                sql += f"    , resident_data AS (\n"
                sql += f"        INSERT INTO data.residents (\n"
                sql += f"            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at\n"
                sql += f"        )\n"
                sql += f"        SELECT \n"
                sql += f"            gen_random_uuid(),\n"
                sql += f"            '{p_name}',\n"
                sql += f"            '{p_phone}',\n"
                sql += f"            '{p_email}',\n"
                sql += f"            '{p_nid}',\n"
                sql += f"            '{p_dob}',\n"
                sql += f"            'ACTIVE',\n"
                sql += f"            NULL,\n"
                sql += f"            now(),\n"
                sql += f"            now()\n"
                sql += f"        FROM unit_data\n"
                sql += f"        ON CONFLICT (phone) DO UPDATE SET\n"
                sql += f"            full_name = EXCLUDED.full_name,\n"
                sql += f"            email = EXCLUDED.email,\n"
                sql += f"            national_id = EXCLUDED.national_id,\n"
                sql += f"            dob = EXCLUDED.dob,\n"
                sql += f"            status = EXCLUDED.status,\n"
                sql += f"            user_id = EXCLUDED.user_id,\n"
                sql += f"            updated_at = now()\n"
                sql += f"        RETURNING id as resident_id\n"
                sql += f"    )\n"
            
            sql += f"    , household_data AS (\n"
            sql += f"        INSERT INTO data.households (\n"
            sql += f"            id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at\n"
            sql += f"        )\n"
            sql += f"        SELECT \n"
            sql += f"            gen_random_uuid(),\n"
            sql += f"            ud.unit_id,\n"
            sql += f"            'OWNER',\n"
            sql += f"            rd.resident_id,\n"
            sql += f"            CURRENT_DATE - INTERVAL '{date_offset}',\n"
            sql += f"            NULL,\n"
            sql += f"            now(),\n"
            sql += f"            now()\n"
            sql += f"        FROM unit_data ud\n"
            sql += f"        CROSS JOIN resident_data rd\n"
            sql += f"        RETURNING id as household_id, primary_resident_id\n"
            sql += f"    )\n"
            sql += f"    INSERT INTO data.household_members (\n"
            sql += f"        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at\n"
            sql += f"    )\n"
            sql += f"    SELECT \n"
            sql += f"        gen_random_uuid(),\n"
            sql += f"        hd.household_id,\n"
            sql += f"        hd.primary_resident_id,\n"
            sql += f"        'Chủ hộ',\n"
            sql += f"        true,\n"
            sql += f"        CURRENT_DATE - INTERVAL '{date_offset}',\n"
            sql += f"        NULL,\n"
            sql += f"        now(),\n"
            sql += f"        now()\n"
            sql += f"    FROM household_data hd;\n"
            sql += f"END $$;\n\n"
            
            # Spouse block
            sql += f"-- {unit_code}: Add spouse ({relation.lower()}) for primary resident\n"
            sql += f"DO $$\nBEGIN\n"
            sql += f"    WITH unit_data AS (\n"
            sql += f"        SELECT u.id as unit_id FROM data.units u\n"
            sql += f"        INNER JOIN data.buildings b ON u.building_id = b.id\n"
            sql += f"        WHERE b.code = '{building_code}' AND u.code = '{unit_code}' LIMIT 1\n"
            sql += f"    )\n"
            sql += f"    , household_info AS (\n"
            sql += f"        SELECT h.id as household_id FROM data.households h\n"
            sql += f"        INNER JOIN unit_data ud ON h.unit_id = ud.unit_id\n"
            sql += f"        WHERE h.primary_resident_id = (\n"
            sql += f"            SELECT r.id FROM data.residents r WHERE r.phone = '{p_phone}' LIMIT 1\n"
            sql += f"        ) LIMIT 1\n"
            sql += f"    )\n"
            sql += f"    , spouse_user_data AS (\n"
            sql += f"        INSERT INTO iam.users (\n"
            sql += f"            id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at\n"
            sql += f"        )\n"
            sql += f"        SELECT \n"
            sql += f"            gen_random_uuid(),\n"
            sql += f"            '{s_username}',\n"
            sql += f"            '{s_email}',\n"
            sql += f"            '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',\n"
            sql += f"            true, NULL, 0, NULL, now(), now()\n"
            sql += f"        FROM household_info\n"
            sql += f"        ON CONFLICT (username) DO UPDATE SET\n"
            sql += f"            email = EXCLUDED.email,\n"
            sql += f"            updated_at = now()\n"
            sql += f"        RETURNING id as user_id\n"
            sql += f"    )\n"
            sql += f"    , spouse_user_role_data AS (\n"
            sql += f"        INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)\n"
            sql += f"        SELECT sud.user_id, 'RESIDENT', now(), 'system'\n"
            sql += f"        FROM spouse_user_data sud\n"
            sql += f"        ON CONFLICT (user_id, role) DO NOTHING\n"
            sql += f"        RETURNING user_id\n"
            sql += f"    )\n"
            sql += f"    , spouse_resident_data AS (\n"
            sql += f"        INSERT INTO data.residents (\n"
            sql += f"            id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at\n"
            sql += f"        )\n"
            sql += f"        SELECT \n"
            sql += f"            gen_random_uuid(),\n"
            sql += f"            '{s_name}',\n"
            sql += f"            '{s_phone}',\n"
            sql += f"            '{s_email}',\n"
            sql += f"            '{s_nid}',\n"
            sql += f"            '{s_dob}',\n"
            sql += f"            'ACTIVE',\n"
            sql += f"            sud.user_id,\n"
            sql += f"            now(),\n"
            sql += f"            now()\n"
            sql += f"        FROM household_info hi\n"
            sql += f"        CROSS JOIN spouse_user_data sud\n"
            sql += f"        ON CONFLICT (phone) DO UPDATE SET\n"
            sql += f"            full_name = EXCLUDED.full_name,\n"
            sql += f"            email = EXCLUDED.email,\n"
            sql += f"            national_id = EXCLUDED.national_id,\n"
            sql += f"            dob = EXCLUDED.dob,\n"
            sql += f"            status = EXCLUDED.status,\n"
            sql += f"            user_id = EXCLUDED.user_id,\n"
            sql += f"            updated_at = now()\n"
            sql += f"        RETURNING id as resident_id\n"
            sql += f"    )\n"
            sql += f"    INSERT INTO data.household_members (\n"
            sql += f"        id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at\n"
            sql += f"    )\n"
            sql += f"    SELECT \n"
            sql += f"        gen_random_uuid(),\n"
            sql += f"        hi.household_id,\n"
            sql += f"        srd.resident_id,\n"
            sql += f"        '{relation}',\n"
            sql += f"        false,\n"
            sql += f"        CURRENT_DATE - INTERVAL '{date_offset}',\n"
            sql += f"        NULL,\n"
            sql += f"        now(),\n"
            sql += f"        now()\n"
            sql += f"    FROM household_info hi\n"
            sql += f"    CROSS JOIN spouse_resident_data srd\n"
            sql += f"    ON CONFLICT (household_id, resident_id) DO NOTHING;\n"
            sql += f"END $$;\n\n"
    
    # Summary query
    sql += """-- Summary query to verify the created data
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
"""
    
    return sql

if __name__ == '__main__':
    sql = generate_sql()
    print(sql)


