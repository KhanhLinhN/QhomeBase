
DELETE FROM iam.role_permissions WHERE role NOT IN ('admin', 'tenant_owner', 'technician', 'supporter', 'account');
DELETE FROM iam.user_roles WHERE role NOT IN ('admin', 'tenant_owner', 'technician', 'supporter', 'account');
DELETE FROM iam.roles WHERE role NOT IN ('admin', 'tenant_owner', 'technician', 'supporter', 'account');


INSERT INTO iam.roles (role, description) VALUES 
    ('admin', 'System Administrator'),
    ('tenant_owner', 'Tenant Owner'),
    ('technician', 'Technician'),
    ('supporter', 'Supporter'),
    ('account', 'Account Manager')
ON CONFLICT (role) DO NOTHING;


UPDATE iam.roles SET description = 'System Administrator' WHERE role = 'admin';
UPDATE iam.roles SET description = 'Tenant Owner' WHERE role = 'tenant_owner';
UPDATE iam.roles SET description = 'Technician' WHERE role = 'technician';
UPDATE iam.roles SET description = 'Supporter' WHERE role = 'supporter';
UPDATE iam.roles SET description = 'Account Manager' WHERE role = 'account';
