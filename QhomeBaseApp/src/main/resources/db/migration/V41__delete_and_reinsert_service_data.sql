-- Flyway migration V41
-- Delete existing service data and reinsert with updated requirements
-- Requirements:
-- 1. BBQ: Thịt 200k, Cồn 10k, Bỏ lửa, Thuê thêm giờ 200k/giờ
-- 2. Bar: Chỉ dùng bar slots (không cần time slot selection)
-- 3. SPA: Chỉ chọn combo (không cần time slot)
-- 4. Pool: Chỉ chọn vé (không cần time slot)
-- 5. Playground -> Electronic Playground: Chỉ chọn vé, giá = vé * số người

-- ============================================
-- STEP 1: Delete existing data
-- ============================================

-- Delete booking items
DELETE FROM qhomebaseapp.service_booking_item;

-- Delete bookings
DELETE FROM qhomebaseapp.service_booking_slot;
DELETE FROM qhomebaseapp.service_booking;

-- Delete service items
DELETE FROM qhomebaseapp.service_option;
DELETE FROM qhomebaseapp.service_combo;
DELETE FROM qhomebaseapp.service_ticket;
DELETE FROM qhomebaseapp.bar_slot;

-- Delete service availability
DELETE FROM qhomebaseapp.service_availability;

-- Delete services (keep categories)
DELETE FROM qhomebaseapp.service WHERE category_id IN (
    SELECT id FROM qhomebaseapp.service_category WHERE code = 'ENTERTAINMENT'
);

-- ============================================
-- STEP 2: Insert Services (5 services)
-- ============================================

-- BBQ Service - 4 zones (A, B, C, D) - 10 people per zone
INSERT INTO qhomebaseapp.service(
    category_id, code, name, description, location,
    price_per_hour, pricing_type, booking_type,
    max_capacity, min_duration_hours, max_duration_hours,
    advance_booking_days, rules, is_active, created_at, updated_at
)
SELECT 
    sc.id,
    'BBQ_ZONE_' || zone_code,
    'Khu nướng BBQ ' || zone_code,
    'Khu nướng BBQ ' || zone_code || ' - Tối đa 10 người',
    'Khu vực tiện ích (Facility Zone)',
    200000.00, -- Base price per hour
    'HOURLY',
    'OPTION_BASED', -- ⭐ Booking type
    10, -- Max capacity
    2, -- Min 2 hours
    8, -- Max 8 hours
    30,
    'Vui lòng dọn dẹp sau khi sử dụng.',
    TRUE,
    NOW(),
    NOW()
FROM qhomebaseapp.service_category sc
CROSS JOIN (SELECT unnest(ARRAY['A', 'B', 'C', 'D']) AS zone_code) zones
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- SPA Service - 4 zones (A, B, C, D) - 4 people per zone
INSERT INTO qhomebaseapp.service(
    category_id, code, name, description, location,
    price_per_hour, pricing_type, booking_type,
    max_capacity, min_duration_hours, max_duration_hours,
    advance_booking_days, rules, is_active, created_at, updated_at
)
SELECT 
    sc.id,
    'SPA_ZONE_' || zone_code,
    'Khu Spa ' || zone_code,
    'Khu Spa ' || zone_code || ' - Tối đa 4 người',
    'Khu vực tiện ích (Facility Zone)',
    0.00, -- Combo-based, không dùng hourly
    'HOURLY',
    'COMBO_BASED', -- ⭐ Booking type
    4, -- Max capacity
    1,
    4,
    30,
    'Vui lòng đến đúng giờ hẹn.',
    TRUE,
    NOW(),
    NOW()
FROM qhomebaseapp.service_category sc
CROSS JOIN (SELECT unnest(ARRAY['A', 'B', 'C', 'D']) AS zone_code) zones
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Pool Service - 4 zones (A, B, C, D) - No capacity limit for tickets
INSERT INTO qhomebaseapp.service(
    category_id, code, name, description, location,
    price_per_hour, pricing_type, booking_type,
    max_capacity, min_duration_hours, max_duration_hours,
    advance_booking_days, rules, is_active, created_at, updated_at
)
SELECT 
    sc.id,
    'POOL_ZONE_' || zone_code,
    'Hồ bơi ' || zone_code,
    'Hồ bơi ' || zone_code,
    'Khu vực tiện ích (Facility Zone)',
    0.00, -- Ticket-based, không dùng hourly
    'HOURLY',
    'TICKET_BASED', -- ⭐ Booking type
    NULL, -- No capacity limit
    1,
    12,
    30,
    'Vui lòng tắm sạch trước khi xuống hồ.',
    TRUE,
    NOW(),
    NOW()
FROM qhomebaseapp.service_category sc
CROSS JOIN (SELECT unnest(ARRAY['A', 'B', 'C', 'D']) AS zone_code) zones
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Electronic Playground Service - 4 zones (A, B, C, D) - 20 people per zone
INSERT INTO qhomebaseapp.service(
    category_id, code, name, description, location,
    price_per_hour, pricing_type, booking_type,
    max_capacity, min_duration_hours, max_duration_hours,
    advance_booking_days, rules, is_active, created_at, updated_at
)
SELECT 
    sc.id,
    'PLAYGROUND_ZONE_' || zone_code,
    'Khu vui chơi điện tử ' || zone_code,
    'Khu vui chơi điện tử ' || zone_code || ' - Tối đa 20 người',
    'Khu vực tiện ích (Facility Zone)',
    0.00, -- Ticket-based, không dùng hourly
    'HOURLY',
    'TICKET_BASED', -- ⭐ Booking type
    20, -- Max capacity
    1,
    12,
    30,
    'Vui lòng giữ gìn vệ sinh chung.',
    TRUE,
    NOW(),
    NOW()
FROM qhomebaseapp.service_category sc
CROSS JOIN (SELECT unnest(ARRAY['A', 'B', 'C', 'D']) AS zone_code) zones
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Bar Service - 4 zones (A, B, C, D) - 30 people per zone
INSERT INTO qhomebaseapp.service(
    category_id, code, name, description, location,
    price_per_hour, pricing_type, booking_type,
    max_capacity, min_duration_hours, max_duration_hours,
    advance_booking_days, rules, is_active, created_at, updated_at
)
SELECT 
    sc.id,
    'BAR_ZONE_' || zone_code,
    'Bar ' || zone_code,
    'Bar ' || zone_code || ' - Tối đa 30 người',
    'Khu vực tiện ích (Facility Zone)',
    0.00, -- Combo-based, không dùng hourly
    'HOURLY',
    'COMBO_BASED', -- ⭐ Booking type
    30, -- Max capacity
    1,
    8,
    30,
    'Vui lòng tuân thủ quy định về đồ uống có cồn.',
    TRUE,
    NOW(),
    NOW()
FROM qhomebaseapp.service_category sc
CROSS JOIN (SELECT unnest(ARRAY['A', 'B', 'C', 'D']) AS zone_code) zones
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- STEP 3: Insert Service Availability
-- ============================================

-- BBQ: Monday to Sunday, 6:00 - 22:00
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, day_num, '06:00:00'::TIME, '22:00:00'::TIME, TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(0, 6) AS day_num
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- SPA: Monday to Sunday, 9:00 - 21:00
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, day_num, '09:00:00'::TIME, '21:00:00'::TIME, TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(0, 6) AS day_num
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Pool: Monday to Sunday, 6:00 - 22:00
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, day_num, '06:00:00'::TIME, '22:00:00'::TIME, TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(0, 6) AS day_num
WHERE s.code LIKE 'POOL_%'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Playground: Monday to Sunday, 8:00 - 22:00
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, day_num, '08:00:00'::TIME, '22:00:00'::TIME, TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(0, 6) AS day_num
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Bar: Monday to Sunday, 17:00 - 02:00 (next day)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, day_num, '17:00:00'::TIME, '02:00:00'::TIME, TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(0, 6) AS day_num
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- ============================================
-- STEP 4: Insert BBQ Options
-- ============================================
-- Thịt: 200.000 VNĐ, Cồn: 10.000 VNĐ, Bỏ lửa

INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_MEAT',
    'Mua hộ thịt',
    'Dịch vụ mua hộ thịt nướng',
    200000.00, -- ⭐ Updated: 200k
    'set',
    FALSE,
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    price = EXCLUDED.price,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_COAL',
    'Thêm than',
    'Thêm than nướng BBQ (20.000 VNĐ/1kg)',
    20000.00,
    'kg',
    FALSE,
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    price = EXCLUDED.price,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_ALCOHOL',
    'Cồn',
    'Cồn để đốt lửa',
    10000.00, -- ⭐ Updated: 10k
    'bình',
    FALSE,
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    price = EXCLUDED.price,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- Delete FIRE option (bỏ lửa)
DELETE FROM qhomebaseapp.service_option WHERE code = 'BBQ_FIRE';

-- Extra hour option: 100.000 VNĐ/giờ
INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_EXTRA_HOUR',
    'Thuê thêm giờ',
    'Thuê thêm giờ (100.000 VNĐ/giờ)',
    100000.00, -- ⭐ Updated: 100k/giờ
    'giờ',
    FALSE,
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    price = EXCLUDED.price,
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- ============================================
-- STEP 5: Insert SPA Combos
-- ============================================

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO1',
    'Combo Cơ bản',
    'Gói dịch vụ spa cơ bản',
    'Massage chân, Massage vai gáy, Tắm hơi',
    60,
    100000.00,
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO2',
    'Combo Tiêu chuẩn',
    'Gói dịch vụ spa tiêu chuẩn',
    'Massage toàn thân, Tắm hơi, Xông hơi',
    90,
    200000.00,
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO3',
    'Combo Cao cấp',
    'Gói dịch vụ spa cao cấp',
    'Massage toàn thân, Tắm hơi, Xông hơi, Chăm sóc da mặt',
    120,
    300000.00,
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO4',
    'Combo VIP',
    'Gói dịch vụ spa VIP',
    'Massage toàn thân, Tắm hơi, Xông hơi, Chăm sóc da mặt, Chăm sóc tóc',
    150,
    400000.00,
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO5',
    'Combo Premium',
    'Gói dịch vụ spa premium',
    'Massage toàn thân, Tắm hơi, Xông hơi, Chăm sóc da mặt, Chăm sóc tóc, Chăm sóc móng',
    180,
    500000.00,
    TRUE,
    5
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

-- ============================================
-- STEP 6: Insert Pool Tickets
-- ============================================

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'POOL_DAY',
    'Vé ngày',
    'DAY',
    8,
    50000.00,
    NULL,
    'Vé sử dụng hồ bơi ban ngày (6:00 - 14:00)',
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'POOL_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'POOL_NIGHT',
    'Vé tối',
    'NIGHT',
    8,
    70000.00,
    NULL,
    'Vé sử dụng hồ bơi ban tối (14:00 - 22:00)',
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'POOL_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

-- ============================================
-- STEP 7: Insert Playground Tickets
-- ============================================
-- Khu vui chơi điện tử: giá = vé * số người

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_1H',
    'Vé gói 1 giờ',
    'HOURLY',
    1,
    80000.00,
    NULL,
    'Vé sử dụng khu vui chơi điện tử 1 giờ',
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_2H',
    'Vé gói 2 giờ',
    'HOURLY',
    2,
    140000.00,
    NULL,
    'Vé sử dụng khu vui chơi điện tử 2 giờ',
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_FULL_DAY',
    'Vé trọn ngày',
    'DAILY',
    12,
    250000.00,
    NULL,
    'Vé sử dụng khu vui chơi điện tử trọn ngày',
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_FAMILY_4',
    'Vé gia đình 4 người',
    'FAMILY',
    4,
    500000.00,
    4,
    'Vé gia đình cho 4 người',
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_FAMILY_3',
    'Vé gia đình 3 người',
    'FAMILY',
    4,
    450000.00,
    3,
    'Vé gia đình cho 3 người',
    TRUE,
    5
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    description = EXCLUDED.description;

-- ============================================
-- STEP 8: Insert Bar Slots
-- ============================================

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_1',
    'SLOT_1',
    '17:00:00'::TIME,
    '19:00:00'::TIME,
    'Giờ "Happy Hour"',
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    note = EXCLUDED.note;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_2',
    'SLOT_2',
    '19:00:00'::TIME,
    '21:00:00'::TIME,
    'Giờ cao điểm',
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    note = EXCLUDED.note;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_3',
    'SLOT_3',
    '21:00:00'::TIME,
    '23:00:00'::TIME,
    'Sau giờ ăn tối',
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    note = EXCLUDED.note;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_4',
    'SLOT_4',
    '23:00:00'::TIME,
    '01:00:00'::TIME,
    'Khuya',
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    note = EXCLUDED.note;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_5',
    'SLOT_5',
    '01:00:00'::TIME,
    '02:00:00'::TIME,
    'Kết thúc',
    TRUE,
    5
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    note = EXCLUDED.note;

-- ============================================
-- STEP 9: Insert Bar Combos
-- ============================================

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'BAR_COMBO1',
    'Chill 1',
    'Combo đồ uống Chill 1',
    '1 tower bia + 1 snack',
    NULL,
    350000.00,
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'BAR_COMBO2',
    'Chill 2',
    'Combo đồ uống Chill 2',
    '1 chai vodka + mixer + trái cây',
    NULL,
    750000.00,
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'BAR_COMBO3',
    'VIP Night',
    'Combo đồ uống VIP Night',
    '1 chai rượu ngoại + sofa zone',
    NULL,
    1200000.00,
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    services_included = EXCLUDED.services_included,
    price = EXCLUDED.price;

