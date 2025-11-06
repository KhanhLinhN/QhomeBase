-- Convert role_permissions table to uppercase to match user_roles table
-- user_roles table stores roles as UPPERCASE (ADMIN, TECHNICIAN, etc.) due to @Enumerated(EnumType.STRING)
-- role_permissions table currently stores roles as lowercase (admin, technician, etc.)
-- This migration converts role_permissions to uppercase for consistency

UPDATE iam.role_permissions 
SET role = UPPER(role)
WHERE role != UPPER(role);

-- Also update roles table if needed
UPDATE iam.roles 
SET role = UPPER(role)
WHERE role != UPPER(role);

-- Update comments to reflect uppercase format
COMMENT ON COLUMN iam.role_permissions.role IS 'Role name in UPPERCASE format (matches UserRole enum name: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';
COMMENT ON COLUMN iam.user_roles.role IS 'Role name in UPPERCASE format (matches UserRole enum name: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';


