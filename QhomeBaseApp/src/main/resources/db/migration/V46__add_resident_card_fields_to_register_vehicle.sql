-- V46__add_resident_card_fields_to_register_vehicle.sql
-- Add fields for Resident Card registration service (Dịch vụ ra vào)

ALTER TABLE qhomebaseapp.register_vehicle
ADD COLUMN IF NOT EXISTS resident_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS apartment_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS building_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS citizen_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_register_vehicle_service_type 
    ON qhomebaseapp.register_vehicle(service_type);

