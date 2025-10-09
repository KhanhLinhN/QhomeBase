ALTER TABLE cs_service.processing_logs
ADD COLUMN request_status VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_processing_logs_request_status ON cs_service.processing_logs (request_status);
ADD COLUMN log_type VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_processing_logs_log_type ON cs_service.processing_logs (log_type);
ADD COLUMN staff_in_charge_name VARCHAR(255);
