-- Convert all roles from lowercase to UPPERCASE to match @Enumerated(EnumType.STRING)
-- @Enumerated(EnumType.STRING) stores enum name exactly: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER
-- Database must store UPPERCASE enum names, not lowercase

-- Step 0: Drop the lowercase constraint to allow UPPERCASE enum names
ALTER TABLE iam.roles DROP CONSTRAINT IF EXISTS ck_roles_code_lower;

-- Step 1: Insert new roles in iam.roles table (UPPERCASE) to ensure foreign key constraints are satisfied
-- Note: Cannot UPDATE primary key directly, so we INSERT new role and UPDATE references, then DELETE old role
INSERT INTO iam.roles (role, description) VALUES
    ('ACCOUNTANT', 'Accountant'),
    ('RESIDENT', 'Resident'),
    ('UNIT_OWNER', 'Unit Owner')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 2: Insert existing roles in UPPERCASE format
INSERT INTO iam.roles (role, description) VALUES
    ('ADMIN', 'Administrator'),
    ('TECHNICIAN', 'Technician'),
    ('SUPPORTER', 'Supporter')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 3: Update user_roles table - convert all roles from lowercase to UPPERCASE
UPDATE iam.user_roles 
SET role = 'ACCOUNTANT' 
WHERE role = 'accountant';

UPDATE iam.user_roles 
SET role = 'ADMIN' 
WHERE role = 'admin';

UPDATE iam.user_roles 
SET role = 'TECHNICIAN' 
WHERE role = 'technician';

UPDATE iam.user_roles 
SET role = 'SUPPORTER' 
WHERE role = 'supporter';

UPDATE iam.user_roles 
SET role = 'RESIDENT' 
WHERE role = 'resident';

UPDATE iam.user_roles 
SET role = 'UNIT_OWNER' 
WHERE role = 'unit_owner';

-- Step 4: Remove 'tenant_owner' roles from user_roles (no longer exists in enum)
-- Note: tenant_owner role entries will be deleted, users should be manually reassigned
DELETE FROM iam.user_roles 
WHERE role IN ('tenant_owner', 'TENANT_OWNER');

-- Step 5: Update role_permissions table - convert all roles from lowercase to UPPERCASE
UPDATE iam.role_permissions 
SET role = 'ACCOUNTANT' 
WHERE role = 'accountant';

UPDATE iam.role_permissions 
SET role = 'ADMIN' 
WHERE role = 'admin';

UPDATE iam.role_permissions 
SET role = 'TECHNICIAN' 
WHERE role = 'technician';

UPDATE iam.role_permissions 
SET role = 'SUPPORTER' 
WHERE role = 'supporter';

UPDATE iam.role_permissions 
SET role = 'RESIDENT' 
WHERE role = 'resident';

UPDATE iam.role_permissions 
SET role = 'UNIT_OWNER' 
WHERE role = 'unit_owner';

DELETE FROM iam.role_permissions 
WHERE role IN ('tenant_owner', 'TENANT_OWNER');

-- Step 6: Clean up roles table - remove lowercase roles (they are now UPPERCASE)
DELETE FROM iam.roles 
WHERE role IN ('tenant_owner', 'TENANT_OWNER', 'accountant', 'admin', 'technician', 'supporter', 'resident', 'unit_owner');

-- Step 7: Verify only valid UPPERCASE roles remain in user_roles and role_permissions
-- Valid roles: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER (UPPERCASE)
DELETE FROM iam.user_roles 
WHERE role NOT IN ('ADMIN', 'ACCOUNTANT', 'TECHNICIAN', 'SUPPORTER', 'RESIDENT', 'UNIT_OWNER');

DELETE FROM iam.role_permissions 
WHERE role NOT IN ('ADMIN', 'ACCOUNTANT', 'TECHNICIAN', 'SUPPORTER', 'RESIDENT', 'UNIT_OWNER');

-- Step 8: Add comments
COMMENT ON COLUMN iam.user_roles.role IS 'Base role name (must match UserRole enum name exactly: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';
COMMENT ON COLUMN iam.role_permissions.role IS 'Base role name (must match UserRole enum name exactly: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';

-- @Enumerated(EnumType.STRING) stores enum name exactly: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER
-- Database must store UPPERCASE enum names, not lowercase

-- Step 0: Drop the lowercase constraint to allow UPPERCASE enum names
ALTER TABLE iam.roles DROP CONSTRAINT IF EXISTS ck_roles_code_lower;

-- Step 1: Insert new roles in iam.roles table (UPPERCASE) to ensure foreign key constraints are satisfied
-- Note: Cannot UPDATE primary key directly, so we INSERT new role and UPDATE references, then DELETE old role
INSERT INTO iam.roles (role, description) VALUES
    ('ACCOUNTANT', 'Accountant'),
    ('RESIDENT', 'Resident'),
    ('UNIT_OWNER', 'Unit Owner')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 2: Insert existing roles in UPPERCASE format
INSERT INTO iam.roles (role, description) VALUES
    ('ADMIN', 'Administrator'),
    ('TECHNICIAN', 'Technician'),
    ('SUPPORTER', 'Supporter')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 3: Update user_roles table - convert all roles from lowercase to UPPERCASE
UPDATE iam.user_roles 
SET role = 'ACCOUNTANT' 
WHERE role = 'accountant';

UPDATE iam.user_roles 
SET role = 'ADMIN' 
WHERE role = 'admin';

UPDATE iam.user_roles 
SET role = 'TECHNICIAN' 
WHERE role = 'technician';

UPDATE iam.user_roles 
SET role = 'SUPPORTER' 
WHERE role = 'supporter';

UPDATE iam.user_roles 
SET role = 'RESIDENT' 
WHERE role = 'resident';

UPDATE iam.user_roles 
SET role = 'UNIT_OWNER' 
WHERE role = 'unit_owner';

-- Step 4: Remove 'tenant_owner' roles from user_roles (no longer exists in enum)
-- Note: tenant_owner role entries will be deleted, users should be manually reassigned
DELETE FROM iam.user_roles 
WHERE role IN ('tenant_owner', 'TENANT_OWNER');

-- Step 5: Update role_permissions table - convert all roles from lowercase to UPPERCASE
UPDATE iam.role_permissions 
SET role = 'ACCOUNTANT' 
WHERE role = 'accountant';

UPDATE iam.role_permissions 
SET role = 'ADMIN' 
WHERE role = 'admin';

UPDATE iam.role_permissions 
SET role = 'TECHNICIAN' 
WHERE role = 'technician';

UPDATE iam.role_permissions 
SET role = 'SUPPORTER' 
WHERE role = 'supporter';

UPDATE iam.role_permissions 
SET role = 'RESIDENT' 
WHERE role = 'resident';

UPDATE iam.role_permissions 
SET role = 'UNIT_OWNER' 
WHERE role = 'unit_owner';

DELETE FROM iam.role_permissions 
WHERE role IN ('tenant_owner', 'TENANT_OWNER');

-- Step 6: Clean up roles table - remove lowercase roles (they are now UPPERCASE)
DELETE FROM iam.roles 
WHERE role IN ('tenant_owner', 'TENANT_OWNER', 'accountant', 'admin', 'technician', 'supporter', 'resident', 'unit_owner');

-- Step 7: Verify only valid UPPERCASE roles remain in user_roles and role_permissions
-- Valid roles: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER (UPPERCASE)
DELETE FROM iam.user_roles 
WHERE role NOT IN ('ADMIN', 'ACCOUNTANT', 'TECHNICIAN', 'SUPPORTER', 'RESIDENT', 'UNIT_OWNER');

DELETE FROM iam.role_permissions 
WHERE role NOT IN ('ADMIN', 'ACCOUNTANT', 'TECHNICIAN', 'SUPPORTER', 'RESIDENT', 'UNIT_OWNER');

-- Step 8: Add comments
COMMENT ON COLUMN iam.user_roles.role IS 'Base role name (must match UserRole enum name exactly: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';
COMMENT ON COLUMN iam.role_permissions.role IS 'Base role name (must match UserRole enum name exactly: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';





