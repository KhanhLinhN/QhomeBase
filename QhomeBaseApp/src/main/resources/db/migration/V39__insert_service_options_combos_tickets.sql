-- Flyway migration V39
-- Insert service options, combos, tickets, and bar slots for different services

-- ============================================
-- BBQ Options (cho dịch vụ BBQ)
-- ============================================
-- Tìm các service có code bắt đầu bằng BBQ_ và thêm options
INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_MEAT',
    'Mua hộ thịt',
    'Dịch vụ mua hộ thịt nướng (tùy chọn)',
    0.00, -- Giá sẽ được tính theo kg hoặc set
    'set',
    FALSE,
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO NOTHING;

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
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_ALCOHOL',
    'Cồn',
    'Cồn để đốt lửa',
    0.00, -- Giá sẽ được tính riêng
    'bình',
    FALSE,
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_FIRE',
    'Lửa',
    'Dịch vụ đốt lửa sẵn',
    0.00,
    'lần',
    FALSE,
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_option(service_id, code, name, description, price, unit, is_required, is_active, sort_order)
SELECT 
    s.id,
    'BBQ_EXTRA_HOUR',
    'Thuê thêm 1 giờ',
    'Thuê thêm 1 giờ (100.000 VNĐ/giờ)',
    100000.00,
    'giờ',
    FALSE,
    TRUE,
    5
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BBQ_%'
ON CONFLICT (service_id, code) DO NOTHING;

-- ============================================
-- SPA Combos (cho dịch vụ SPA)
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
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO2',
    'Combo Tiêu chuẩn',
    'Gói dịch vụ spa tiêu chuẩn',
    'Massage toàn thân, Xông hơi, Tắm thảo dược',
    90,
    250000.00,
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'SPA_COMBO3',
    'Combo Cao cấp',
    'Gói dịch vụ spa cao cấp',
    'Massage trị liệu, Xông hơi thảo dược, Đắp mặt nạ, Chăm sóc da',
    120,
    500000.00,
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'SPA_%'
ON CONFLICT (service_id, code) DO NOTHING;

-- ============================================
-- Pool Tickets (cho dịch vụ Pool)
-- ============================================
INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, description, is_active, sort_order)
SELECT 
    s.id,
    'POOL_DAY',
    'Vé ngày',
    'DAY',
    8.00, -- 8 giờ (từ sáng đến tối)
    50000.00,
    'Vé bơi ban ngày (từ 6:00 - 18:00)',
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'POOL_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, description, is_active, sort_order)
SELECT 
    s.id,
    'POOL_NIGHT',
    'Vé tối',
    'NIGHT',
    4.00, -- 4 giờ (tối)
    70000.00,
    'Vé bơi ban đêm (từ 18:00 - 22:00)',
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'POOL_%'
ON CONFLICT (service_id, code) DO NOTHING;

-- ============================================
-- Playground Tickets (cho dịch vụ Playground)
-- ============================================
INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_1H',
    'Vé gói 1 giờ',
    'HOURLY',
    1.00,
    80000.00,
    'Vé chơi 1 giờ tại khu vui chơi',
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_2H',
    'Vé gói 2 giờ',
    'HOURLY',
    2.00,
    140000.00,
    'Vé chơi 2 giờ tại khu vui chơi',
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_DAILY',
    'Vé trọn ngày',
    'DAILY',
    8.00,
    250000.00,
    'Vé chơi trọn ngày tại khu vui chơi',
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_FAMILY_4',
    'Vé gói gia đình 4 người',
    'FAMILY',
    NULL, -- Không giới hạn thời gian trong ngày
    500000.00,
    4,
    'Vé gói gia đình cho 4 người (chơi trọn ngày)',
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_ticket(service_id, code, name, ticket_type, duration_hours, price, max_people, description, is_active, sort_order)
SELECT 
    s.id,
    'PLAYGROUND_FAMILY_3',
    'Vé gói gia đình 3 người',
    'FAMILY',
    NULL,
    450000.00, -- Giảm 50.000 so với 4 người
    3,
    'Vé gói gia đình cho 3 người (chơi trọn ngày)',
    TRUE,
    5
FROM qhomebaseapp.service s
WHERE s.code LIKE 'PLAYGROUND_%'
ON CONFLICT (service_id, code) DO NOTHING;

-- ============================================
-- Bar Slots (cho dịch vụ Bar)
-- ============================================
INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_1',
    'Giờ Happy Hour',
    '17:00:00'::TIME,
    '19:00:00'::TIME,
    'Giờ "Happy Hour"',
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_2',
    'Giờ cao điểm',
    '19:00:00'::TIME,
    '21:00:00'::TIME,
    'Giờ cao điểm',
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_3',
    'Sau giờ ăn tối',
    '21:00:00'::TIME,
    '23:00:00'::TIME,
    'Sau giờ ăn tối',
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_4',
    'Khuya',
    '23:00:00'::TIME,
    '01:00:00'::TIME,
    'Khuya',
    TRUE,
    4
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.bar_slot(service_id, code, name, start_time, end_time, note, is_active, sort_order)
SELECT 
    s.id,
    'SLOT_5',
    'Kết thúc',
    '01:00:00'::TIME,
    '02:00:00'::TIME,
    'Kết thúc',
    TRUE,
    5
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

-- ============================================
-- Bar Combos (cho dịch vụ Bar)
-- ============================================
INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'BAR_COMBO1',
    'Chill 1',
    'Combo đồ uống nhẹ',
    '1 tower bia + 1 snack',
    NULL, -- Không giới hạn thời gian
    350000.00,
    TRUE,
    1
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'BAR_COMBO2',
    'Chill 2',
    'Combo đồ uống trung bình',
    '1 chai vodka + mixer + trái cây',
    NULL,
    750000.00,
    TRUE,
    2
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

INSERT INTO qhomebaseapp.service_combo(service_id, code, name, description, services_included, duration_minutes, price, is_active, sort_order)
SELECT 
    s.id,
    'BAR_COMBO3',
    'VIP Night',
    'Combo đồ uống cao cấp',
    '1 chai rượu ngoại + sofa zone',
    NULL,
    1200000.00,
    TRUE,
    3
FROM qhomebaseapp.service s
WHERE s.code LIKE 'BAR_%'
ON CONFLICT (service_id, code) DO NOTHING;

