-- Flyway migration V32
-- Rename register_service_request table to register_vehicle
-- Rename register_service_image table to register_vehicle_image

-- Rename main table
ALTER TABLE qhomebaseapp.register_service_request 
RENAME TO register_vehicle;

-- Rename image table
ALTER TABLE qhomebaseapp.register_service_image 
RENAME TO register_vehicle_image;

-- Rename foreign key column in register_vehicle_image
ALTER TABLE qhomebaseapp.register_vehicle_image 
RENAME COLUMN register_request_id TO register_vehicle_id;

-- Rename indexes if they exist
-- Note: PostgreSQL automatically renames indexes when table is renamed
-- But we need to check and rename if needed
DO $$
BEGIN
    -- Rename index for register_vehicle.user_id if it exists
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_user_id' AND tablename = 'register_vehicle') THEN
        ALTER INDEX qhomebaseapp.idx_user_id RENAME TO idx_register_vehicle_user_id;
    END IF;
    
    -- Rename any other indexes that might exist
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'register_service_image') THEN
        -- Find and rename indexes (if any were created explicitly)
        -- This is handled automatically by PostgreSQL when table is renamed
        NULL;
    END IF;
END $$;

-- Update any comments or constraints if needed
COMMENT ON TABLE qhomebaseapp.register_vehicle IS 'Vehicle registration requests';
COMMENT ON TABLE qhomebaseapp.register_vehicle_image IS 'Vehicle registration images';

