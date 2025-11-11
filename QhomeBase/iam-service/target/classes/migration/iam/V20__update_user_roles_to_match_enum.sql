-- Update user_roles table to match UserRole enum values
-- Enum values: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER
-- getRoleName() returns lowercase: admin, accountant, technician, supporter, resident, unit_owner

-- Step 1: First, insert new roles in iam.roles table to ensure foreign key constraints are satisfied
-- Note: Cannot UPDATE primary key directly, so we INSERT new role and UPDATE references, then DELETE old role
-- Note: is_global column was removed in V10, so we only insert role and description
INSERT INTO iam.roles (role, description) VALUES
    ('accountant', 'Accountant'),
    ('resident', 'Resident'),
    ('unit_owner', 'Unit Owner')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 2: Now update user_roles table (foreign key constraint satisfied - accountant exists)
UPDATE iam.user_roles 
SET role = 'accountant' 
WHERE role = 'account';

-- Step 3: Remove 'tenant_owner' roles from user_roles (no longer exists in enum)
-- Note: tenant_owner role entries will be deleted, users should be manually reassigned
DELETE FROM iam.user_roles 
WHERE role = 'tenant_owner';

-- Step 4: Update role_permissions table
UPDATE iam.role_permissions 
SET role = 'accountant' 
WHERE role = 'account';

DELETE FROM iam.role_permissions 
WHERE role = 'tenant_owner';

-- Step 5: Clean up roles table - remove invalid roles (tenant_owner, account if still exists)
DELETE FROM iam.roles 
WHERE role IN ('tenant_owner', 'account');

-- Step 6: Verify only valid roles remain in user_roles and role_permissions
-- Valid roles: admin, accountant, technician, supporter, resident, unit_owner
DELETE FROM iam.user_roles 
WHERE role NOT IN ('admin', 'accountant', 'technician', 'supporter', 'resident', 'unit_owner');

DELETE FROM iam.role_permissions 
WHERE role NOT IN ('admin', 'accountant', 'technician', 'supporter', 'resident', 'unit_owner');

-- Step 7: Add comments
COMMENT ON COLUMN iam.user_roles.role IS 'Base role name (must match UserRole enum getRoleName() values: admin, accountant, technician, supporter, resident, unit_owner)';
COMMENT ON COLUMN iam.role_permissions.role IS 'Base role name (must match UserRole enum getRoleName() values: admin, accountant, technician, supporter, resident, unit_owner)';








