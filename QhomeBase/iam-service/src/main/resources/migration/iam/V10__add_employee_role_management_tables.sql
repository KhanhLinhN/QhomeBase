ALTER TABLE iam.roles DROP COLUMN IF EXISTS is_global;

ALTER TABLE iam.users 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now(),
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

CREATE TABLE IF NOT EXISTS iam.role_assignment_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    role_name TEXT NOT NULL,
    action TEXT NOT NULL CHECK (action IN ('ASSIGN', 'REMOVE')),
    performed_by TEXT NOT NULL,
    reason TEXT,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS ix_users_active ON iam.users(is_active);
CREATE INDEX IF NOT EXISTS ix_users_created_at ON iam.users(created_at);

CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_user_tenant ON iam.user_tenant_roles(user_id, tenant_id);
CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_tenant_role ON iam.user_tenant_roles(tenant_id, role);
CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_granted_at ON iam.user_tenant_roles(granted_at);

CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_user ON iam.role_assignment_audit(user_id);
CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_tenant ON iam.role_assignment_audit(tenant_id);
CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_performed_at ON iam.role_assignment_audit(performed_at);

INSERT INTO iam.permissions (code, description) VALUES 
('iam.employee.read', 'View employee information'),
('iam.employee.role.assign', 'Assign roles to employees'),
('iam.employee.role.remove', 'Remove roles from employees'),
('iam.employee.role.bulk_assign', 'Bulk assign roles to employees'),
('iam.employee.export', 'Export employee list'),
('iam.employee.import', 'Import employee list'),
('iam.permission.manage', 'Manage user permissions'),
('base.building.manage', 'Manage building information'),
('base.unit.manage', 'Manage units/apartments'),
('base.resident.manage', 'Manage residents'),
('base.resident.approve', 'Approve resident applications'),
('finance.fee.manage', 'Manage fees and billing')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.roles (role, description) VALUES 
('staff', 'Building Staff - Basic building operations')
ON CONFLICT (role) DO NOTHING;

INSERT INTO iam.role_permissions (role, permission_code) VALUES 
('staff', 'iam.employee.read'),
('staff', 'iam.user.read'),
('staff', 'iam.role.read'),
('staff', 'iam.role.permission.read'),
('staff', 'iam.permission.read'),

('account', 'iam.employee.read'),
('account', 'finance.fee.manage'),

('technician', 'iam.user.read'),
('technician', 'iam.role.read'),
('technician', 'iam.role.permission.read'),
('technician', 'iam.permission.read'),

('supporter', 'iam.user.read'),
('supporter', 'iam.role.read'),
('supporter', 'iam.role.permission.read'),
('supporter', 'iam.permission.read')
ON CONFLICT (role, permission_code) DO NOTHING;

INSERT INTO iam.users (id, username, email, password_hash, active, last_login_at, failed_attempts, locked_until) VALUES
('dd0e8400-e29b-41d4-a716-446655440008', 'staff_member', 'staff@qhomebase.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, now(), 0, null),
('ee0e8400-e29b-41d4-a716-446655440009', 'account_manager', 'account@qhomebase.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, now(), 0, null),
('ff0e8400-e29b-41d4-a716-446655440010', 'tech_specialist', 'tech@qhomebase.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, now(), 0, null),
('000e8400-e29b-41d4-a716-446655440011', 'support_agent', 'support@qhomebase.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', true, now(), 0, null)
ON CONFLICT (email) DO NOTHING;

INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
SELECT u.id, r.role, now(), 'system'
FROM iam.users u
CROSS JOIN (VALUES 
    ('dd0e8400-e29b-41d4-a716-446655440008', 'staff'),
    ('ee0e8400-e29b-41d4-a716-446655440009', 'account'),
    ('ff0e8400-e29b-41d4-a716-446655440010', 'technician'),
    ('000e8400-e29b-41d4-a716-446655440011', 'supporter')
) AS r(user_id, role)
WHERE u.id = r.user_id::uuid
ON CONFLICT (user_id, role) DO NOTHING;

