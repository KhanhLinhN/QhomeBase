-- V49__rename_bar_slot_to_service_slot.sql
-- Rename bar_slot table to service_slot for generic use across all services

-- Rename table
ALTER TABLE qhomebaseapp.bar_slot RENAME TO service_slot;

-- Rename constraints
ALTER TABLE qhomebaseapp.service_slot 
    RENAME CONSTRAINT fk_bar_slot_service TO fk_service_slot_service;

ALTER TABLE qhomebaseapp.service_slot 
    RENAME CONSTRAINT unique_bar_slot_code TO unique_service_slot_code;

-- Rename indexes
DROP INDEX IF EXISTS qhomebaseapp.idx_bar_slot_service_id;
DROP INDEX IF EXISTS qhomebaseapp.idx_bar_slot_code;

CREATE INDEX IF NOT EXISTS idx_service_slot_service_id 
    ON qhomebaseapp.service_slot(service_id);
    
CREATE INDEX IF NOT EXISTS idx_service_slot_code 
    ON qhomebaseapp.service_slot(code);

-- Update comment
COMMENT ON TABLE qhomebaseapp.service_slot IS 'Các slot giờ cố định cho services (Bar, BBQ, etc.) - Admin có thể CRUD';

