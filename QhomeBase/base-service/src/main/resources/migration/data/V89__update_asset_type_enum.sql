-- V89: Update asset_type enum to only keep: AIR_CONDITIONER, KITCHEN, WATER_HEATER, FURNITURE, OTHER
-- Remove: REFRIGERATOR, WASHING_MACHINE, FAN, TELEVISION

-- Step 1: Update existing assets with removed types to OTHER
UPDATE data.assets
SET asset_type = 'OTHER'
WHERE asset_type IN ('REFRIGERATOR', 'WASHING_MACHINE', 'FAN', 'TELEVISION');

-- Step 2: Drop and recreate the enum type
-- First, change all columns using the enum to text temporarily
ALTER TABLE data.assets ALTER COLUMN asset_type TYPE TEXT USING asset_type::TEXT;

-- Check if asset_inspections table exists and uses asset_type (only update if it exists)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'data' AND table_name = 'asset_inspections') THEN
        -- If asset_inspections has asset_type column, update it too
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'data' AND table_name = 'asset_inspections' AND column_name = 'asset_type') THEN
            ALTER TABLE data.asset_inspections ALTER COLUMN asset_type TYPE TEXT USING asset_type::TEXT;
        END IF;
    END IF;
END $$;

-- Drop the old enum
DROP TYPE IF EXISTS data.asset_type;

-- Create new enum with only the 5 types
CREATE TYPE data.asset_type AS ENUM (
    'AIR_CONDITIONER',
    'KITCHEN',
    'WATER_HEATER',
    'FURNITURE',
    'OTHER'
);

-- Change the columns back to use the new enum
ALTER TABLE data.assets 
    ALTER COLUMN asset_type TYPE data.asset_type USING asset_type::data.asset_type;

-- Update asset_inspections if it exists
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'data' AND table_name = 'asset_inspections') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'data' AND table_name = 'asset_inspections' AND column_name = 'asset_type') THEN
            ALTER TABLE data.asset_inspections 
                ALTER COLUMN asset_type TYPE data.asset_type USING asset_type::data.asset_type;
        END IF;
    END IF;
END $$;

-- Add comment
COMMENT ON TYPE data.asset_type IS 'Loại tài sản: Điều hòa, Bếp, Nóng lạnh, Nội thất, Khác';

