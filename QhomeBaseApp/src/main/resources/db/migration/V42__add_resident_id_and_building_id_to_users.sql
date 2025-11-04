-- Add resident_id and building_id columns to users table
-- These UUIDs are used to identify residents in the admin service (customer-interaction-service)

ALTER TABLE qhomebaseapp.users
ADD COLUMN IF NOT EXISTS resident_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS building_id VARCHAR(255);

COMMENT ON COLUMN qhomebaseapp.users.resident_id IS 'UUID của cư dân từ admin system, dùng để gọi API news và notifications';
COMMENT ON COLUMN qhomebaseapp.users.building_id IS 'UUID của tòa nhà từ admin system, dùng để gọi API notifications';

