-- Add Trello integration fields to maintenance_requests table
ALTER TABLE data.maintenance_requests
ADD COLUMN IF NOT EXISTS trello_card_id TEXT,
ADD COLUMN IF NOT EXISTS assigned_staff_id UUID;

CREATE INDEX IF NOT EXISTS idx_maintenance_requests_trello_card ON data.maintenance_requests(trello_card_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_assigned_staff ON data.maintenance_requests(assigned_staff_id);

