-- V50__add_service_type_fields.sql
-- Add service_type and type_display_name columns to service table for generic service type management

ALTER TABLE qhomebaseapp.service
ADD COLUMN IF NOT EXISTS service_type VARCHAR(50);

ALTER TABLE qhomebaseapp.service
ADD COLUMN IF NOT EXISTS type_display_name VARCHAR(255);

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_service_service_type 
    ON qhomebaseapp.service(service_type);

-- Update comment
COMMENT ON COLUMN qhomebaseapp.service.service_type IS 'Loại dịch vụ để group (BBQ, SPA, POOL, BAR, TENNIS, etc.)';
COMMENT ON COLUMN qhomebaseapp.service.type_display_name IS 'Tên hiển thị cho loại dịch vụ (ví dụ: "Nướng BBQ", "Hồ bơi", "Spa")';

