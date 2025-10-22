-- Assign mike_maintenance to tenant as tenant_owner
-- This fixes the login issue where mike_maintenance has no tenant access

-- Use the same demo tenant ID from V13
-- 2b5b2af5-9431-4649-8144-35830d866826

INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('33333333-3333-3333-3333-333333333333', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_owner', now(), 'system');
