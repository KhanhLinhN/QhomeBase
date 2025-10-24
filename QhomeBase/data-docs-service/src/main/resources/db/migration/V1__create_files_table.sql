CREATE SCHEMA IF NOT EXISTS files;

CREATE TABLE files.file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    file_path TEXT NOT NULL,
    file_url TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES base.tenants(id),
    CONSTRAINT fk_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES iam.users(id)
);

CREATE INDEX idx_file_metadata_tenant_id ON files.file_metadata(tenant_id);
CREATE INDEX idx_file_metadata_category ON files.file_metadata(category);
CREATE INDEX idx_file_metadata_uploaded_by ON files.file_metadata(uploaded_by);
CREATE INDEX idx_file_metadata_uploaded_at ON files.file_metadata(uploaded_at);
CREATE INDEX idx_file_metadata_deleted ON files.file_metadata(is_deleted) WHERE is_deleted = false;

