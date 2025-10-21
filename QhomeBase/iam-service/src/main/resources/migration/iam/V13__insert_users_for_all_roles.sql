-- Insert test users for all base roles with plain-text passwords (temporary)
-- Requires the temporary raw password check in AuthService

-- Common demo tenant used across seed data
-- 2b5b2af5-9431-4649-8144-35830d866826

-- Admin user
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('c10e8400-e29b-41d4-a716-446655440100', 'admin_test', 'admin_test@qhomebase.com', 'password', true, now(), 0, null);
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('c10e8400-e29b-41d4-a716-446655440100', 'admin', now(), 'system');
INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('c10e8400-e29b-41d4-a716-446655440100', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_admin', now(), 'system');

-- Tenant Owner user
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('c20e8400-e29b-41d4-a716-446655440101', 'tenant_owner_test', 'tenant_owner_test@qhomebase.com', 'password', true, now(), 0, null);
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('c20e8400-e29b-41d4-a716-446655440101', 'tenant_owner', now(), 'system');
INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('c20e8400-e29b-41d4-a716-446655440101', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_manager', now(), 'system');

-- Technician user
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('c30e8400-e29b-41d4-a716-446655440102', 'technician_test', 'technician_test@qhomebase.com', 'password', true, now(), 0, null);
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('c30e8400-e29b-41d4-a716-446655440102', 'technician', now(), 'system');
INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('c30e8400-e29b-41d4-a716-446655440102', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_tech', now(), 'system');

-- Supporter user
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('c40e8400-e29b-41d4-a716-446655440103', 'supporter_test', 'supporter_test@qhomebase.com', 'password', true, now(), 0, null);
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('c40e8400-e29b-41d4-a716-446655440103', 'supporter', now(), 'system');
INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('c40e8400-e29b-41d4-a716-446655440103', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_support', now(), 'system');

-- Account user
INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('c50e8400-e29b-41d4-a716-446655440104', 'account_test', 'account_test@qhomebase.com', 'password', true, now(), 0, null);
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('c50e8400-e29b-41d4-a716-446655440104', 'account', now(), 'system');
-- There is no dedicated tenant_account role in seed; align to tenant_support as in V8
INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('c50e8400-e29b-41d4-a716-446655440104', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_support', now(), 'system');


