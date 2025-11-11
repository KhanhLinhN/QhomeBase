-- V22: Insert users for test residents (primary residents/chủ hộ)
-- These users will be linked to residents in base-service migration
-- Password hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi (password: password123)

-- User 1: Nguyễn Văn A (Primary - Unit A101)
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440110'::uuid,
    'nguyenvana',
    'nguyenvana@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    true,
    NULL,
    0,
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    active = EXCLUDED.active,
    updated_at = now();

-- User 2: Trần Thị D (Primary - Unit A102)
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440111'::uuid,
    'tranthid',
    'tranthid@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    true,
    NULL,
    0,
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    active = EXCLUDED.active,
    updated_at = now();

-- User 3: Lê Văn F (Primary - Unit A201)
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440112'::uuid,
    'levanf',
    'levanf@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    true,
    NULL,
    0,
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    active = EXCLUDED.active,
    updated_at = now();

-- User 4: Phạm Văn H (Primary - Unit B101)
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440113'::uuid,
    'phamvanh',
    'phamvanh@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    true,
    NULL,
    0,
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    active = EXCLUDED.active,
    updated_at = now();

-- Ensure RESIDENT role exists in roles table (uppercase format)
INSERT INTO iam.roles (role, description)
VALUES ('RESIDENT', 'Resident')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Assign RESIDENT role to all users (uppercase format per V21)
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440110'::uuid, 'RESIDENT', now(), 'system'),
    ('550e8400-e29b-41d4-a716-446655440111'::uuid, 'RESIDENT', now(), 'system'),
    ('550e8400-e29b-41d4-a716-446655440112'::uuid, 'RESIDENT', now(), 'system'),
    ('550e8400-e29b-41d4-a716-446655440113'::uuid, 'RESIDENT', now(), 'system')
ON CONFLICT (user_id, role) DO NOTHING;

