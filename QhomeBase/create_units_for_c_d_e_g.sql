-- Script to create units for buildings C, D, E, G
-- This script must be run BEFORE seed_households_fixed.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ensure buildings C, D, E, G exist
INSERT INTO data.buildings (
    id,
    code,
    name,
    address,
    status,
    number_of_floors,
    is_deleted,
    created_by,
    updated_by,
    created_at,
    updated_at
) VALUES
    (
        gen_random_uuid(),
        'C',
        'Tòa nhà C',
        '789 Đường Trần Văn C, Phường 3, Quận 3, Hà Nội',
        'ACTIVE',
        15,
        false,
        'admin',
        'admin',
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'D',
        'Tòa nhà D',
        '321 Đường Phạm Văn D, Phường 4, Quận 4, Hà Nội',
        'ACTIVE',
        8,
        false,
        'admin',
        'admin',
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'E',
        'Tòa nhà E',
        '654 Đường Hoàng Văn E, Phường 5, Quận 5, Hà Nội',
        'ACTIVE',
        20,
        false,
        'admin',
        'admin',
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'G',
        'Tòa nhà G',
        '147 Đường Đặng Văn G, Phường 7, Quận 7, Hà Nội',
        'ACTIVE',
        14,
        false,
        'admin',
        'admin',
        now(),
        now()
    )
ON CONFLICT (code) DO NOTHING;

-- Create units for building C (C1---01 to C3---05)
INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'C1---01',
    1,
    50.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'C1---02',
    1,
    55.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'C2---03',
    2,
    60.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'C2---04',
    2,
    65.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'C3---05',
    3,
    70.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

-- Create units for building D (D1---01 to D3---05)
INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'D1---01',
    1,
    50.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'D1---02',
    1,
    55.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'D2---03',
    2,
    60.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'D2---04',
    2,
    65.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'D3---05',
    3,
    70.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

-- Create units for building E (E1---01 to E3---05)
INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'E1---01',
    1,
    50.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'E1---02',
    1,
    55.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'E2---03',
    2,
    60.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'E2---04',
    2,
    65.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'E3---05',
    3,
    70.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

-- Create units for building G (G1---01 to G3---05)
INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'G1---01',
    1,
    50.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'G1---02',
    1,
    55.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'G2---03',
    2,
    60.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'G2---04',
    2,
    65.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    'G3---05',
    3,
    70.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

-- Verify created units
SELECT 
    b.code as building_code,
    u.code as unit_code,
    u.floor,
    u.area_m2,
    u.bedrooms,
    u.status
FROM data.units u
INNER JOIN data.buildings b ON u.building_id = b.id
WHERE b.code IN ('C', 'D', 'E', 'G')
ORDER BY b.code, u.code;




