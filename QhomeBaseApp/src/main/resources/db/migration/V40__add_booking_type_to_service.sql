-- Flyway migration V40
-- Add booking_type field to service table to make the system flexible
-- Admin can now manage services without code changes

-- Add booking_type column to service table
ALTER TABLE qhomebaseapp.service 
ADD COLUMN IF NOT EXISTS booking_type VARCHAR(50);

-- Add comment to explain booking types
COMMENT ON COLUMN qhomebaseapp.service.booking_type IS 
'Booking type determines how the service is booked:
- COMBO_BASED: Service requires selecting a combo (e.g., SPA, Bar, KFC)
- TICKET_BASED: Service requires selecting a ticket (e.g., Pool, Playground)
- OPTION_BASED: Service allows selecting options (e.g., BBQ with meat, charcoal, etc.)
- STANDARD: Standard booking with hourly or per-session pricing (default)

When booking_type is NULL, system defaults to STANDARD behavior.';

-- Update existing services with booking_type based on their code prefix
-- This is a one-time migration to set booking_type for existing services
UPDATE qhomebaseapp.service
SET booking_type = CASE
    WHEN code LIKE 'BBQ_%' THEN 'OPTION_BASED'
    WHEN code LIKE 'SPA_%' THEN 'COMBO_BASED'
    WHEN code LIKE 'POOL_%' THEN 'TICKET_BASED'
    WHEN code LIKE 'PLAYGROUND_%' THEN 'TICKET_BASED'
    WHEN code LIKE 'BAR_%' THEN 'COMBO_BASED'
    ELSE 'STANDARD'
END
WHERE booking_type IS NULL;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_service_booking_type 
ON qhomebaseapp.service(booking_type);

