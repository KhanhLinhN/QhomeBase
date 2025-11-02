-- V1: Create assets schema and assets table
-- This migration creates the asset maintenance schema and assets table for managing building assets

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS asset;

-- Create enum types for asset management
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'asset_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'asset')) THEN
        CREATE TYPE asset.asset_type AS ENUM (
            'ELEVATOR',
            'GENERATOR',
            'AIR_CONDITIONER',
            'WATER_PUMP',
            'SECURITY_SYSTEM',
            'FIRE_SAFETY',
            'CCTV',
            'INTERCOM',
            'GATE_BARRIER',
            'SWIMMING_POOL',
            'GYM_EQUIPMENT',
            'PLAYGROUND',
            'PARKING_SYSTEM',
            'GARDEN_IRRIGATION',
            'LIGHTING_SYSTEM',
            'WIFI_SYSTEM',
            'SOLAR_PANEL',
            'WASTE_MANAGEMENT',
            'MAINTENANCE_TOOL',
            'OTHER'
        );
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'asset_status' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'asset')) THEN
        CREATE TYPE asset.asset_status AS ENUM (
            'ACTIVE',
            'INACTIVE',
            'MAINTENANCE',
            'REPAIRING',
            'REPLACED',
            'DECOMMISSIONED'
        );
    END IF;
END$$;

-- Create assets table
CREATE TABLE IF NOT EXISTS asset.assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL REFERENCES data.buildings(id) ON DELETE CASCADE,
    unit_id UUID REFERENCES data.units(id) ON DELETE SET NULL, -- NULL for building-level assets, set for unit-level assets
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    asset_type asset.asset_type NOT NULL,
    status asset.asset_status NOT NULL DEFAULT 'ACTIVE',
    location TEXT, -- Location within building (e.g., "Lobby", "Basement", "Floor 5", "Parking Level 2", "Unit A-101")
    manufacturer TEXT,
    model TEXT,
    serial_number TEXT,
    purchase_date DATE,
    purchase_price NUMERIC(15,2), -- Purchase cost in VND or currency
    current_value NUMERIC(15,2), -- Current estimated value
    replacement_cost NUMERIC(15,2), -- Estimated replacement cost
    warranty_expiry_date DATE,
    warranty_provider TEXT, -- Warranty service provider name
    warranty_contact TEXT, -- Warranty service contact info
    installation_date DATE,
    last_maintenance_date DATE, -- Last maintenance/service date
    next_maintenance_date DATE, -- Next scheduled maintenance date
    maintenance_interval_days INTEGER, -- Maintenance interval in days (e.g., 90, 180, 365)
    expected_lifespan_years INTEGER, -- Expected lifespan in years
    decommission_date DATE, -- Date when asset was decommissioned
    supplier_name TEXT, -- Purchase supplier/vendor name
    supplier_contact TEXT, -- Supplier contact information
    service_provider_name TEXT, -- Maintenance service provider name
    service_provider_contact TEXT, -- Service provider contact information
    qr_code TEXT, -- QR code for asset tracking
    barcode TEXT, -- Barcode for asset tracking
    tag_number TEXT, -- Physical tag number
    image_urls JSONB, -- Array of image URLs for asset photos
    description TEXT,
    notes TEXT, -- Additional notes/comments
    specifications JSONB, -- Additional specifications as JSON (e.g., capacity, power consumption, etc.)
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,
    CONSTRAINT ck_assets_warranty_date CHECK (warranty_expiry_date IS NULL OR purchase_date IS NULL OR warranty_expiry_date >= purchase_date),
    CONSTRAINT ck_assets_installation_date CHECK (installation_date IS NULL OR purchase_date IS NULL OR installation_date >= purchase_date),
    CONSTRAINT ck_assets_next_maintenance_date CHECK (next_maintenance_date IS NULL OR last_maintenance_date IS NULL OR next_maintenance_date >= last_maintenance_date),
    CONSTRAINT ck_assets_purchase_price CHECK (purchase_price IS NULL OR purchase_price >= 0),
    CONSTRAINT ck_assets_current_value CHECK (current_value IS NULL OR current_value >= 0),
    CONSTRAINT ck_assets_replacement_cost CHECK (replacement_cost IS NULL OR replacement_cost >= 0),
    CONSTRAINT ck_assets_maintenance_interval CHECK (maintenance_interval_days IS NULL OR maintenance_interval_days > 0),
    CONSTRAINT ck_assets_lifespan CHECK (expected_lifespan_years IS NULL OR expected_lifespan_years > 0)
    -- Note: Unit must belong to the same building. This is enforced at application layer via service validation
);

-- Create unique indexes for code uniqueness (using partial indexes for building-level vs unit-level)
-- Code must be unique within a building for building-level assets (unit_id IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_assets_building_code ON asset.assets(building_id, code) 
WHERE unit_id IS NULL AND is_deleted = FALSE;

-- Code must be unique within a unit for unit-level assets (unit_id IS NOT NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_assets_unit_code ON asset.assets(unit_id, code) 
WHERE unit_id IS NOT NULL AND is_deleted = FALSE;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_assets_building_id ON asset.assets(building_id);
CREATE INDEX IF NOT EXISTS idx_assets_unit_id ON asset.assets(unit_id) WHERE unit_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_status ON asset.assets(status);
CREATE INDEX IF NOT EXISTS idx_assets_asset_type ON asset.assets(asset_type);
CREATE INDEX IF NOT EXISTS idx_assets_is_deleted ON asset.assets(is_deleted);
CREATE INDEX IF NOT EXISTS idx_assets_building_status ON asset.assets(building_id, status);
CREATE INDEX IF NOT EXISTS idx_assets_unit_status ON asset.assets(unit_id, status) WHERE unit_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_warranty_expiry ON asset.assets(warranty_expiry_date) WHERE warranty_expiry_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_next_maintenance ON asset.assets(next_maintenance_date) WHERE next_maintenance_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_status_maintenance ON asset.assets(status, next_maintenance_date) 
WHERE status IN ('ACTIVE', 'MAINTENANCE') AND next_maintenance_date IS NOT NULL;

-- Create partial index for active assets (most commonly queried)
CREATE INDEX IF NOT EXISTS idx_assets_active_building ON asset.assets(building_id, asset_type) 
WHERE is_deleted = FALSE AND status = 'ACTIVE' AND unit_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_assets_active_unit ON asset.assets(unit_id, asset_type) 
WHERE is_deleted = FALSE AND status = 'ACTIVE' AND unit_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON SCHEMA asset IS 'Schema for asset maintenance service - manages building infrastructure assets';
COMMENT ON TABLE asset.assets IS 'Stores information about building assets and infrastructure equipment. Can be building-level (unit_id = NULL) or unit-level (unit_id != NULL)';
COMMENT ON COLUMN asset.assets.building_id IS 'Foreign key to data.buildings table';
COMMENT ON COLUMN asset.assets.unit_id IS 'Foreign key to data.units table. NULL for building-level assets (elevators, generators, etc.), set for unit-level assets (AC units, water heaters, etc.)';
COMMENT ON COLUMN asset.assets.code IS 'Unique asset code within a building (if unit_id is NULL) or within a unit (if unit_id is NOT NULL). Examples: "ELEVATOR-01", "GEN-01" for building-level, "AC-01", "WH-01" for unit-level';
COMMENT ON COLUMN asset.assets.asset_type IS 'Type of asset (elevator, generator, AC, etc.)';
COMMENT ON COLUMN asset.assets.location IS 'Physical location within the building or unit (e.g., "Lobby", "Basement", "Floor 5", "Unit A-101", "Kitchen")';
COMMENT ON COLUMN asset.assets.purchase_price IS 'Purchase cost of the asset';
COMMENT ON COLUMN asset.assets.current_value IS 'Current estimated value of the asset';
COMMENT ON COLUMN asset.assets.replacement_cost IS 'Estimated cost to replace the asset';
COMMENT ON COLUMN asset.assets.last_maintenance_date IS 'Date of last maintenance/service performed';
COMMENT ON COLUMN asset.assets.next_maintenance_date IS 'Next scheduled maintenance/service date';
COMMENT ON COLUMN asset.assets.maintenance_interval_days IS 'Recommended maintenance interval in days';
COMMENT ON COLUMN asset.assets.expected_lifespan_years IS 'Expected useful lifespan in years';
COMMENT ON COLUMN asset.assets.qr_code IS 'QR code for asset identification and tracking';
COMMENT ON COLUMN asset.assets.barcode IS 'Barcode for asset identification';
COMMENT ON COLUMN asset.assets.tag_number IS 'Physical tag/label number attached to asset';
COMMENT ON COLUMN asset.assets.image_urls IS 'Array of image URLs for asset photos (stored as JSONB)';
COMMENT ON COLUMN asset.assets.specifications IS 'Additional specifications stored as JSONB (flexible schema)';

