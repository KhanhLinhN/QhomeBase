INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'admin', 'admin@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', true, now(), 0, null);

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'tenant_owner', 'owner@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', true, now(), 0, null);

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('770e8400-e29b-41d4-a716-446655440002', 'technician', 'tech@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', true, now(), 0, null);

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('880e8400-e29b-41d4-a716-446655440003', 'supporter', 'support@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', true, now(), 0, null);

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('990e8400-e29b-41d4-a716-446655440004', 'account', 'account@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', true, now(), 0, null);

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('aa0e8400-e29b-41d4-a716-446655440005', 'locked_user', 'locked@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', false, now() - interval '1 hour', 5, now() + interval '1 hour');

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('bb0e8400-e29b-41d4-a716-446655440006', 'inactive_user', 'inactive@qhomebase.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', false, now() - interval '1 day', 0, null);

INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'admin', now(), 'system'),
('660e8400-e29b-41d4-a716-446655440001', 'tenant_owner', now(), 'system'),
('770e8400-e29b-41d4-a716-446655440002', 'technician', now(), 'system'),
('880e8400-e29b-41d4-a716-446655440003', 'supporter', now(), 'system'),
('990e8400-e29b-41d4-a716-446655440004', 'account', now(), 'system');

INSERT INTO iam.tenant_roles (tenant_id, role, base_role, description) VALUES
('2b5b2af5-9431-4649-8144-35830d866826', 'tenant_admin', 'admin', 'Tenant Administrator'),
('2b5b2af5-9431-4649-8144-35830d866826', 'tenant_manager', 'tenant_owner', 'Tenant Manager'),
('2b5b2af5-9431-4649-8144-35830d866826', 'tenant_tech', 'technician', 'Tenant Technician'),
('2b5b2af5-9431-4649-8144-35830d866826', 'tenant_support', 'supporter', 'Tenant Support');

INSERT INTO iam.user_tenant_roles (user_id, tenant_id, role, granted_at, granted_by) VALUES
('550e8400-e29b-41d4-a716-446655440000', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_admin', now(), 'system'),
('660e8400-e29b-41d4-a716-446655440001', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_manager', now(), 'system'),
('770e8400-e29b-41d4-a716-446655440002', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_tech', now(), 'system'),
('880e8400-e29b-41d4-a716-446655440003', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_support', now(), 'system'),
('990e8400-e29b-41d4-a716-446655440004', '2b5b2af5-9431-4649-8144-35830d866826', 'tenant_support', now(), 'system');
