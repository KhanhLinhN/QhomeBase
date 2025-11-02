-- Flyway migration V34
-- Insert test data for BBQ Service (Entertainment category)
-- Multiple BBQ zones (khu vực) with different locations

-- Insert BBQ Service - Zone A (Khu cạnh tòa A)
INSERT INTO qhomebaseapp.service(
    category_id, 
    code, 
    name, 
    description, 
    location, 
    map_url,
    price_per_hour, 
    pricing_type, 
    max_capacity, 
    min_duration_hours, 
    max_duration_hours,
    advance_booking_days,
    rules,
    is_active
)
SELECT 
    sc.id,
    'BBQ_ZONE_A',
    'Khu nướng BBQ - Tòa A',
    'Khu nướng BBQ với 5 bếp nướng và khu vực ngồi rộng rãi, đủ cho 8 người. Có mái che và quạt thông gió.',
    'Khu vực cạnh tòa A, tầng trệt, gần khu vui chơi trẻ em',
    'https://maps.example.com/bbq-zone-a',
    200000.00, -- 200,000 VNĐ/giờ
    'HOURLY',
    8, -- Tối đa 8 người
    2, -- Tối thiểu 2 giờ
    6, -- Tối đa 6 giờ
    30, -- Đặt trước tối đa 30 ngày
    '1. Giữ vệ sinh khu vực sau khi sử dụng
2. Tắt bếp nướng trước khi rời đi
3. Không để lại rác thải
4. Sử dụng dụng cụ nướng có sẵn
5. Tôn trọng giờ đã đặt, không sử dụng quá giờ',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert BBQ Service - Zone B (Khu cạnh tòa B)
INSERT INTO qhomebaseapp.service(
    category_id, 
    code, 
    name, 
    description, 
    location, 
    map_url,
    price_per_hour, 
    pricing_type, 
    max_capacity, 
    min_duration_hours, 
    max_duration_hours,
    advance_booking_days,
    rules,
    is_active
)
SELECT 
    sc.id,
    'BBQ_ZONE_B',
    'Khu nướng BBQ - Tòa B',
    'Khu nướng BBQ với 4 bếp nướng và khu vực ngồi, đủ cho 6 người. Có mái che và hệ thống chiếu sáng.',
    'Khu vực cạnh tòa B, tầng trệt, gần bể bơi',
    'https://maps.example.com/bbq-zone-b',
    180000.00, -- 180,000 VNĐ/giờ
    'HOURLY',
    6, -- Tối đa 6 người
    2, -- Tối thiểu 2 giờ
    6, -- Tối đa 6 giờ
    30,
    '1. Giữ vệ sinh khu vực sau khi sử dụng
2. Tắt bếp nướng trước khi rời đi
3. Không để lại rác thải
4. Sử dụng dụng cụ nướng có sẵn
5. Tôn trọng giờ đã đặt, không sử dụng quá giờ',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert BBQ Service - Zone C (Khu cạnh tòa C)
INSERT INTO qhomebaseapp.service(
    category_id, 
    code, 
    name, 
    description, 
    location, 
    map_url,
    price_per_hour, 
    pricing_type, 
    max_capacity, 
    min_duration_hours, 
    max_duration_hours,
    advance_booking_days,
    rules,
    is_active
)
SELECT 
    sc.id,
    'BBQ_ZONE_C',
    'Khu nướng BBQ - Tòa C',
    'Khu nướng BBQ với 5 bếp nướng và khu vực ngồi rộng rãi, đủ cho 7 người. Có mái che, quạt và nhạc nền.',
    'Khu vực cạnh tòa C, tầng trệt, gần khu thể thao',
    'https://maps.example.com/bbq-zone-c',
    220000.00, -- 220,000 VNĐ/giờ
    'HOURLY',
    7, -- Tối đa 7 người
    2, -- Tối thiểu 2 giờ
    6, -- Tối đa 6 giờ
    30,
    '1. Giữ vệ sinh khu vực sau khi sử dụng
2. Tắt bếp nướng trước khi rời đi
3. Không để lại rác thải
4. Sử dụng dụng cụ nướng có sẵn
5. Tôn trọng giờ đã đặt, không sử dụng quá giờ',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert BBQ Service - Zone D (Khu cạnh tòa D)
INSERT INTO qhomebaseapp.service(
    category_id, 
    code, 
    name, 
    description, 
    location, 
    map_url,
    price_per_hour, 
    pricing_type, 
    max_capacity, 
    min_duration_hours, 
    max_duration_hours,
    advance_booking_days,
    rules,
    is_active
)
SELECT 
    sc.id,
    'BBQ_ZONE_D',
    'Khu nướng BBQ - Tòa D',
    'Khu nướng BBQ với 4 bếp nướng và khu vực ngồi, đủ cho 5 người. Vị trí yên tĩnh, phù hợp cho nhóm nhỏ.',
    'Khu vực cạnh tòa D, tầng trệt, gần công viên',
    'https://maps.example.com/bbq-zone-d',
    150000.00, -- 150,000 VNĐ/giờ
    'HOURLY',
    5, -- Tối đa 5 người
    2, -- Tối thiểu 2 giờ
    6, -- Tối đa 6 giờ
    30,
    '1. Giữ vệ sinh khu vực sau khi sử dụng
2. Tắt bếp nướng trước khi rời đi
3. Không để lại rác thải
4. Sử dụng dụng cụ nướng có sẵn
5. Tôn trọng giờ đã đặt, không sử dụng quá giờ',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert Service Availability for BBQ Zone A
-- Monday to Friday: 14:00 - 22:00 (2 PM - 10 PM)
-- Saturday & Sunday: 10:00 - 22:00 (10 AM - 10 PM)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    1, -- Monday
    '14:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    2, -- Tuesday
    '14:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    3, -- Wednesday
    '14:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    4, -- Thursday
    '14:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    5, -- Friday
    '14:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    6, -- Saturday
    '10:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT 
    s.id,
    0, -- Sunday
    '10:00:00'::TIME,
    '22:00:00'::TIME,
    TRUE
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert Service Availability for BBQ Zone B (same schedule)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
UNION ALL
SELECT s.id, 2, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
UNION ALL
SELECT s.id, 3, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
UNION ALL
SELECT s.id, 4, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
UNION ALL
SELECT s.id, 5, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
UNION ALL
SELECT s.id, 6, '10:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
UNION ALL
SELECT s.id, 0, '10:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_B'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert Service Availability for BBQ Zone C (same schedule)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
UNION ALL
SELECT s.id, 2, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
UNION ALL
SELECT s.id, 3, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
UNION ALL
SELECT s.id, 4, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
UNION ALL
SELECT s.id, 5, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
UNION ALL
SELECT s.id, 6, '10:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
UNION ALL
SELECT s.id, 0, '10:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_C'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert Service Availability for BBQ Zone D (same schedule)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
UNION ALL
SELECT s.id, 2, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
UNION ALL
SELECT s.id, 3, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
UNION ALL
SELECT s.id, 4, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
UNION ALL
SELECT s.id, 5, '14:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
UNION ALL
SELECT s.id, 6, '10:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
UNION ALL
SELECT s.id, 0, '10:00:00'::TIME, '22:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'BBQ_ZONE_D'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert test bookings to demonstrate availability checking
-- These bookings will make certain time slots unavailable
-- Note: This assumes there's at least one user in the system (user_id = 1)
-- Booking 1: BBQ Zone A - Today + 2 days, 14:00-16:00 (2 hours)
-- Booking 2: BBQ Zone B - Today + 3 days, 15:00-17:00 (2 hours)
-- Booking 3: BBQ Zone A - Today + 5 days, 16:00-18:00 (2 hours)

-- Note: These insert statements will only work if:
-- 1. There's at least one user in the users table (we'll use user_id = 1)
-- 2. The services exist (inserted above)
-- We use INSERT ... ON CONFLICT DO NOTHING to avoid errors if run multiple times

-- Insert test booking for BBQ Zone A (14:00-16:00, 2 days from now)
INSERT INTO qhomebaseapp.service_booking(
    service_id, 
    user_id, 
    booking_date, 
    start_time, 
    end_time, 
    duration_hours,
    number_of_people,
    purpose,
    total_amount,
    payment_status,
    status,
    terms_accepted,
    created_at,
    updated_at
)
SELECT 
    s.id,
    1, -- Assuming user_id = 1 exists
    CURRENT_DATE + INTERVAL '2 days',
    '14:00:00'::TIME,
    '16:00:00'::TIME,
    2.00,
    4,
    'Tiệc BBQ gia đình',
    s.price_per_hour * 2,
    'PAID',
    'APPROVED',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
AND EXISTS (SELECT 1 FROM qhomebaseapp.users WHERE id = 1)
LIMIT 1;

-- Insert test booking for BBQ Zone B (15:00-17:00, 3 days from now)
INSERT INTO qhomebaseapp.service_booking(
    service_id, 
    user_id, 
    booking_date, 
    start_time, 
    end_time, 
    duration_hours,
    number_of_people,
    purpose,
    total_amount,
    payment_status,
    status,
    terms_accepted,
    created_at,
    updated_at
)
SELECT 
    s.id,
    1,
    CURRENT_DATE + INTERVAL '3 days',
    '15:00:00'::TIME,
    '17:00:00'::TIME,
    2.00,
    5,
    'Tiệc BBQ bạn bè',
    s.price_per_hour * 2,
    'PAID',
    'APPROVED',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_B'
AND EXISTS (SELECT 1 FROM qhomebaseapp.users WHERE id = 1)
LIMIT 1;

-- Insert test booking for BBQ Zone A (16:00-18:00, 5 days from now)
INSERT INTO qhomebaseapp.service_booking(
    service_id, 
    user_id, 
    booking_date, 
    start_time, 
    end_time, 
    duration_hours,
    number_of_people,
    purpose,
    total_amount,
    payment_status,
    status,
    terms_accepted,
    created_at,
    updated_at
)
SELECT 
    s.id,
    1,
    CURRENT_DATE + INTERVAL '5 days',
    '16:00:00'::TIME,
    '18:00:00'::TIME,
    2.00,
    6,
    'Tiệc BBQ sinh nhật',
    s.price_per_hour * 2,
    'PAID',
    'APPROVED',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM qhomebaseapp.service s
WHERE s.code = 'BBQ_ZONE_A'
AND EXISTS (SELECT 1 FROM qhomebaseapp.users WHERE id = 1)
LIMIT 1;

-- Insert booking slots for the above bookings
-- This ensures the slot tracking system works correctly
INSERT INTO qhomebaseapp.service_booking_slot(booking_id, service_id, slot_date, start_time, end_time, created_at)
SELECT 
    b.id,
    b.service_id,
    b.booking_date,
    b.start_time,
    b.end_time,
    b.created_at
FROM qhomebaseapp.service_booking b
WHERE b.booking_date >= CURRENT_DATE
AND b.id NOT IN (SELECT DISTINCT booking_id FROM qhomebaseapp.service_booking_slot WHERE booking_id IS NOT NULL)
ON CONFLICT DO NOTHING;

