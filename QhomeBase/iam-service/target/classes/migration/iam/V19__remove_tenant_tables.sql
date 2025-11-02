-- Drop function that depends on tenant tables
DROP FUNCTION IF EXISTS iam.get_user_permissions_in_tenant(UUID, UUID) CASCADE;

-- Drop tenant-related tables in correct order (respecting foreign keys)
DROP TABLE IF EXISTS iam.user_tenant_denies CASCADE;
DROP TABLE IF EXISTS iam.user_tenant_grants CASCADE;
DROP TABLE IF EXISTS iam.user_tenant_roles CASCADE;
DROP TABLE IF EXISTS iam.tenant_role_permissions CASCADE;
DROP TABLE IF EXISTS iam.tenant_roles CASCADE;

-- Drop tenant_id column from role_assignment_audit table
ALTER TABLE IF EXISTS iam.role_assignment_audit 
DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Drop indexes related to tenant_id in role_assignment_audit
DROP INDEX IF EXISTS iam.ix_role_assignment_audit_tenant;

COMMENT ON TABLE iam.user_roles IS 'User base roles - no longer uses tenant-based roles';
COMMENT ON TABLE iam.role_permissions IS 'Base role permissions - applies globally, not per tenant';

DROP FUNCTION IF EXISTS iam.get_user_permissions_in_tenant(UUID, UUID) CASCADE;

-- Drop tenant-related tables in correct order (respecting foreign keys)
DROP TABLE IF EXISTS iam.user_tenant_denies CASCADE;
DROP TABLE IF EXISTS iam.user_tenant_grants CASCADE;
DROP TABLE IF EXISTS iam.user_tenant_roles CASCADE;
DROP TABLE IF EXISTS iam.tenant_role_permissions CASCADE;
DROP TABLE IF EXISTS iam.tenant_roles CASCADE;

-- Drop tenant_id column from role_assignment_audit table
ALTER TABLE IF EXISTS iam.role_assignment_audit 
DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Drop indexes related to tenant_id in role_assignment_audit
DROP INDEX IF EXISTS iam.ix_role_assignment_audit_tenant;

COMMENT ON TABLE iam.user_roles IS 'User base roles - no longer uses tenant-based roles';
COMMENT ON TABLE iam.role_permissions IS 'Base role permissions - applies globally, not per tenant';





