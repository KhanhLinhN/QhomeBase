-- =====================================================
-- Script SQL để sửa các vấn đề migration
-- Chạy script này trong PostgreSQL database của bạn
-- =====================================================

-- =====================================================
-- 1. Thêm cột progress_notes vào maintenance_requests (V84)
-- =====================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'progress_notes'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN progress_notes TEXT;
        RAISE NOTICE 'Đã thêm cột progress_notes';
    ELSE
        RAISE NOTICE 'Cột progress_notes đã tồn tại';
    END IF;
END $$;

COMMENT ON COLUMN data.maintenance_requests.progress_notes IS 'Notes from staff/admin during repair progress (when status = IN_PROGRESS)';

-- =====================================================
-- 2. Thêm các cột payment vào maintenance_requests (V85)
-- =====================================================
DO $$
BEGIN
    -- payment_status
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_status'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_status VARCHAR(50) DEFAULT 'UNPAID';
        RAISE NOTICE 'Đã thêm cột payment_status';
    END IF;

    -- payment_amount
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_amount'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_amount NUMERIC(15, 2);
        RAISE NOTICE 'Đã thêm cột payment_amount';
    END IF;

    -- payment_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_date'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_date TIMESTAMPTZ;
        RAISE NOTICE 'Đã thêm cột payment_date';
    END IF;

    -- payment_gateway
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_gateway'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_gateway VARCHAR(50);
        RAISE NOTICE 'Đã thêm cột payment_gateway';
    END IF;

    -- vnpay_transaction_ref
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'vnpay_transaction_ref'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN vnpay_transaction_ref VARCHAR(255);
        RAISE NOTICE 'Đã thêm cột vnpay_transaction_ref';
    END IF;
END $$;

-- Tạo indexes cho payment fields
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_payment_status 
    ON data.maintenance_requests(payment_status);
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_vnpay_transaction_ref 
    ON data.maintenance_requests(vnpay_transaction_ref);

-- Comments cho payment fields
COMMENT ON COLUMN data.maintenance_requests.payment_status IS 'Payment status: UNPAID, PAID, FAILED';
COMMENT ON COLUMN data.maintenance_requests.payment_amount IS 'Amount paid for maintenance request';
COMMENT ON COLUMN data.maintenance_requests.payment_date IS 'Date when payment was completed';
COMMENT ON COLUMN data.maintenance_requests.payment_gateway IS 'Payment gateway used (VNPAY, etc.)';
COMMENT ON COLUMN data.maintenance_requests.vnpay_transaction_ref IS 'VNPay transaction reference for tracking payment';

-- =====================================================
-- 3. Tạo enum asset_type và bảng assets (V86)
-- =====================================================
-- Tạo enum asset_type
DO $$ BEGIN
    CREATE TYPE data.asset_type AS ENUM (
        'AIR_CONDITIONER',
        'KITCHEN',
        'WATER_HEATER',
        'FURNITURE',
        'OTHER'
    );
    RAISE NOTICE 'Đã tạo enum asset_type';
EXCEPTION
    WHEN duplicate_object THEN 
        RAISE NOTICE 'Enum asset_type đã tồn tại';
END $$;

-- Tạo bảng assets
CREATE TABLE IF NOT EXISTS data.assets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id         UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    asset_type      data.asset_type NOT NULL,
    asset_code      TEXT NOT NULL,
    name            TEXT,
    brand           TEXT,
    model           TEXT,
    serial_number   TEXT,
    description     TEXT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    installed_at    DATE,
    removed_at      DATE,
    warranty_until  DATE,
    purchase_price  NUMERIC(14, 2),
    purchase_date   DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_asset_code UNIQUE (asset_code),
    CONSTRAINT ck_assets_period CHECK (removed_at IS NULL OR removed_at >= installed_at),
    CONSTRAINT ck_assets_warranty CHECK (warranty_until IS NULL OR warranty_until >= installed_at)
);

-- Tạo indexes cho assets
CREATE INDEX IF NOT EXISTS idx_assets_unit ON data.assets (unit_id);
CREATE INDEX IF NOT EXISTS idx_assets_type ON data.assets (asset_type);
CREATE INDEX IF NOT EXISTS idx_assets_active ON data.assets (active);
CREATE INDEX IF NOT EXISTS idx_assets_code ON data.assets (asset_code);

-- Comments cho assets
COMMENT ON TABLE data.assets IS 'Tài sản của căn hộ (điều hòa, bếp, tủ lạnh, ...)';
COMMENT ON COLUMN data.assets.asset_type IS 'Loại tài sản';
COMMENT ON COLUMN data.assets.asset_code IS 'Mã tài sản (unique)';
COMMENT ON COLUMN data.assets.name IS 'Tên tài sản';
COMMENT ON COLUMN data.assets.brand IS 'Thương hiệu';
COMMENT ON COLUMN data.assets.model IS 'Model';
COMMENT ON COLUMN data.assets.serial_number IS 'Số serial';
COMMENT ON COLUMN data.assets.description IS 'Mô tả chi tiết';
COMMENT ON COLUMN data.assets.installed_at IS 'Ngày lắp đặt';
COMMENT ON COLUMN data.assets.removed_at IS 'Ngày tháo gỡ';
COMMENT ON COLUMN data.assets.warranty_until IS 'Bảo hành đến ngày';
COMMENT ON COLUMN data.assets.purchase_price IS 'Giá mua';
COMMENT ON COLUMN data.assets.purchase_date IS 'Ngày mua';

-- =====================================================
-- 4. Tạo enum inspection_status và bảng asset_inspections (V87)
-- =====================================================
-- Tạo enum inspection_status
DO $$ BEGIN
    CREATE TYPE data.inspection_status AS ENUM (
        'PENDING',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    );
    RAISE NOTICE 'Đã tạo enum inspection_status';
EXCEPTION
    WHEN duplicate_object THEN 
        RAISE NOTICE 'Enum inspection_status đã tồn tại';
END $$;

-- Tạo bảng asset_inspections
CREATE TABLE IF NOT EXISTS data.asset_inspections (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id         UUID NOT NULL,
    unit_id             UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    inspection_date     DATE NOT NULL,
    status              data.inspection_status NOT NULL DEFAULT 'PENDING',
    inspector_name      TEXT,
    inspector_notes     TEXT,
    completed_at        TIMESTAMPTZ,
    completed_by        UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    CONSTRAINT uq_asset_inspection_contract UNIQUE (contract_id)
);

-- Tạo bảng asset_inspection_items
CREATE TABLE IF NOT EXISTS data.asset_inspection_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id       UUID NOT NULL REFERENCES data.asset_inspections(id) ON DELETE CASCADE,
    asset_id            UUID NOT NULL REFERENCES data.assets(id) ON DELETE CASCADE,
    condition_status    TEXT,
    notes               TEXT,
    checked             BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at          TIMESTAMPTZ,
    checked_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tạo indexes cho asset_inspections
CREATE INDEX IF NOT EXISTS idx_asset_inspections_contract ON data.asset_inspections (contract_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_unit ON data.asset_inspections (unit_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_status ON data.asset_inspections (status);
CREATE INDEX IF NOT EXISTS idx_asset_inspection_items_inspection ON data.asset_inspection_items (inspection_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspection_items_asset ON data.asset_inspection_items (asset_id);

-- Comments cho asset_inspections
COMMENT ON TABLE data.asset_inspections IS 'Kiểm tra thiết bị khi hợp đồng hết hạn';
COMMENT ON COLUMN data.asset_inspections.contract_id IS 'ID hợp đồng (từ data-docs-service)';
COMMENT ON COLUMN data.asset_inspections.unit_id IS 'ID căn hộ';
COMMENT ON COLUMN data.asset_inspections.inspection_date IS 'Ngày kiểm tra';
COMMENT ON COLUMN data.asset_inspections.status IS 'Trạng thái kiểm tra';
COMMENT ON COLUMN data.asset_inspections.inspector_name IS 'Tên người kiểm tra';
COMMENT ON COLUMN data.asset_inspections.inspector_notes IS 'Ghi chú của người kiểm tra';

COMMENT ON TABLE data.asset_inspection_items IS 'Danh sách thiết bị cần kiểm tra';
COMMENT ON COLUMN data.asset_inspection_items.asset_id IS 'ID thiết bị cần kiểm tra';
COMMENT ON COLUMN data.asset_inspection_items.condition_status IS 'Tình trạng thiết bị (GOOD, DAMAGED, MISSING, etc.)';
COMMENT ON COLUMN data.asset_inspection_items.checked IS 'Đã kiểm tra chưa';
COMMENT ON COLUMN data.asset_inspection_items.checked_at IS 'Thời gian kiểm tra';
COMMENT ON COLUMN data.asset_inspection_items.checked_by IS 'Người kiểm tra';
