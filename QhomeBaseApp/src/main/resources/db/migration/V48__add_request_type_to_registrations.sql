-- V48__add_request_type_to_registrations.sql
-- Add request_type column to resident_card_registration and register_vehicle tables

-- Add request_type to resident_card_registration table
ALTER TABLE qhomebaseapp.resident_card_registration
ADD COLUMN IF NOT EXISTS request_type VARCHAR(50) NOT NULL DEFAULT 'NEW_CARD';

-- Add request_type to register_vehicle table
ALTER TABLE qhomebaseapp.register_vehicle
ADD COLUMN IF NOT EXISTS request_type VARCHAR(50) DEFAULT 'NEW_CARD';

-- Add comment for clarity
COMMENT ON COLUMN qhomebaseapp.resident_card_registration.request_type IS 'Loại yêu cầu: NEW_CARD (Làm thẻ mới) hoặc REPLACE_CARD (Cấp lại thẻ bị mất)';
COMMENT ON COLUMN qhomebaseapp.register_vehicle.request_type IS 'Loại yêu cầu: NEW_CARD (Làm thẻ mới) hoặc REPLACE_CARD (Cấp lại thẻ bị mất)';

