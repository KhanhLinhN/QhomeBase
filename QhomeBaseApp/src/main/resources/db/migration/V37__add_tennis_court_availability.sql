-- Flyway migration V37
-- Add service availability schedule for TENNIS_COURT_BOOKING service
-- This ensures the service can be booked

-- Insert Service Availability for TENNIS_COURT_BOOKING
-- Monday to Sunday: 6:00 - 22:00 (6 AM - 10 PM)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    day_num,
    '06:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(0, 6) AS day_num
WHERE s.code = 'TENNIS_COURT_BOOKING'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Verify the service exists and has correct pricing type
-- Update if needed
UPDATE qhomebaseapp.service
SET 
    pricing_type = 'PER_SESSION',
    price_per_session = COALESCE(price_per_session, 250000.00),
    is_active = TRUE
WHERE code = 'TENNIS_COURT_BOOKING'
AND (pricing_type IS NULL OR pricing_type != 'PER_SESSION');

