/* Add tracking fields for resend reminder workflow */
ALTER TABLE data.cleaning_requests
ADD COLUMN last_resent_at TIMESTAMP WITH TIME ZONE NULL,
ADD COLUMN resend_alert_sent BOOLEAN NOT NULL DEFAULT FALSE;

