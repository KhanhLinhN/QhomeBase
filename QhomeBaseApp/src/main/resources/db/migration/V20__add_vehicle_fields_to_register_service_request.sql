-- ============================================
-- V20__add_vehicle_fields_to_register_service_request.sql
-- Migrate table: qhomebaseapp.register_service_request
-- Purpose: Add vehicle information fields for vehicle card registration
-- ============================================

ALTER TABLE qhomebaseapp.register_service_request
    ADD COLUMN IF NOT EXISTS vehicle_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS license_plate VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vehicle_brand VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vehicle_color VARCHAR(100),
    ADD COLUMN IF NOT EXISTS image_url TEXT;
