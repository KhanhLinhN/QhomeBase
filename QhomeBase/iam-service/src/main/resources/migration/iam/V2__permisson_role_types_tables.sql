CREATE SCHEMA IF NOT EXISTS iam;

CREATE TABLE IF NOT EXISTS iam.permissions (
                                               code TEXT PRIMARY KEY,
                                               description TEXT
);

CREATE TABLE IF NOT EXISTS iam.role_permissions (
                                                    role TEXT NOT NULL REFERENCES iam.roles(role) ON DELETE CASCADE,
    permission_code TEXT NOT NULL REFERENCES iam.permissions(code) ON DELETE RESTRICT,
    PRIMARY KEY (role, permission_code)
    );

CREATE TABLE IF NOT EXISTS iam.tenant_roles (
                                                tenant_id UUID NOT NULL,
                                                role TEXT NOT NULL,
                                                base_role TEXT NULL REFERENCES iam.roles(role),
    description TEXT,
    PRIMARY KEY (tenant_id, role)
    );

CREATE TABLE IF NOT EXISTS iam.tenant_role_permissions (
                                                           tenant_id UUID NOT NULL,
                                                           role TEXT NOT NULL,
                                                           permission_code TEXT NOT NULL REFERENCES iam.permissions(code),
    PRIMARY KEY (tenant_id, role, permission_code),
    FOREIGN KEY (tenant_id, role)
    REFERENCES iam.tenant_roles(tenant_id, role) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS iam.user_tenant_roles (
                                                     user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    role TEXT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT,
    PRIMARY KEY (user_id, tenant_id, role),
    FOREIGN KEY (tenant_id, role)
    REFERENCES iam.tenant_roles(tenant_id, role) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS ix_role_permissions_role
    ON iam.role_permissions(role);

CREATE INDEX IF NOT EXISTS ix_role_permissions_perm
    ON iam.role_permissions(permission_code);

CREATE INDEX IF NOT EXISTS ix_tenant_role_permissions_perm
    ON iam.tenant_role_permissions(permission_code);

CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_user
    ON iam.user_tenant_roles(user_id);

CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_tenant
    ON iam.user_tenant_roles(tenant_id);
