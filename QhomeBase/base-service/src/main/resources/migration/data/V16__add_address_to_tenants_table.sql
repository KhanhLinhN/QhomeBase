-- Add address column to tenants table
ALTER TABLE data.tenants 
ADD COLUMN address TEXT;

-- Add comment for the new column
COMMENT ON COLUMN data.tenants.address IS 'Physical address of the tenant organization';
