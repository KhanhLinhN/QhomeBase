-- Flyway migration V36
-- Insert Entertainment Services: BBQ, SPA, Khu vui chơi, Bar
-- Each service has 4 zones: A, B, C, D

-- BBQ Service - 10 people per zone
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
    code_val,
    name_val,
    desc_val,
    location_val,
    'https://maps.example.com/' || LOWER(code_val),
    price_val,
    'HOURLY',
    10, -- BBQ: 10 people max per zone
    2, -- Min 2 hours
    6, -- Max 6 hours
    30,
    '1. Giữ vệ sinh khu vực sau khi sử dụng
2. Tắt bếp nướng trước khi rời đi
3. Không để lại rác thải
4. Sử dụng dụng cụ nướng có sẵn
5. Tôn trọng giờ đã đặt, không sử dụng quá giờ
6. Số người tối đa: 10 người/khu',
    TRUE
FROM qhomebaseapp.service_category sc,
(VALUES
    ('BBQ_ZONE_A', 'Khu nướng BBQ - Khu A', 'Khu nướng BBQ với 5 bếp nướng và khu vực ngồi rộng rãi, đủ cho 10 người. Có mái che và quạt thông gió.', 'Khu vực cạnh tòa A, tầng trệt', 200000.00),
    ('BBQ_ZONE_B', 'Khu nướng BBQ - Khu B', 'Khu nướng BBQ với 5 bếp nướng và khu vực ngồi rộng rãi, đủ cho 10 người. Có mái che và hệ thống chiếu sáng.', 'Khu vực cạnh tòa B, tầng trệt', 200000.00),
    ('BBQ_ZONE_C', 'Khu nướng BBQ - Khu C', 'Khu nướng BBQ với 5 bếp nướng và khu vực ngồi rộng rãi, đủ cho 10 người. Có mái che và quạt thông gió.', 'Khu vực cạnh tòa C, tầng trệt', 200000.00),
    ('BBQ_ZONE_D', 'Khu nướng BBQ - Khu D', 'Khu nướng BBQ với 5 bếp nướng và khu vực ngồi rộng rãi, đủ cho 10 người. Có mái che và quạt thông gió.', 'Khu vực cạnh tòa D, tầng trệt', 200000.00)
) AS t(code_val, name_val, desc_val, location_val, price_val)
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- SPA Service - 4 people per zone
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
    code_val,
    name_val,
    desc_val,
    location_val,
    'https://maps.example.com/' || LOWER(code_val),
    price_val,
    'HOURLY',
    4, -- SPA: 4 people max per zone
    1, -- Min 1 hour
    4, -- Max 4 hours
    30,
    '1. Vui lòng đến đúng giờ đã đặt
2. Giữ im lặng trong khu vực spa
3. Không sử dụng điện thoại trong khu vực thư giãn
4. Tôn trọng không gian riêng tư của người khác
5. Số người tối đa: 4 người/khu',
    TRUE
FROM qhomebaseapp.service_category sc,
(VALUES
    ('SPA_ZONE_A', 'Khu Spa - Khu A', 'Khu spa với phòng xông hơi, massage và khu vực thư giãn. Dành cho tối đa 4 người.', 'Tầng 2, tòa A', 350000.00),
    ('SPA_ZONE_B', 'Khu Spa - Khu B', 'Khu spa với phòng xông hơi, massage và khu vực thư giãn. Dành cho tối đa 4 người.', 'Tầng 2, tòa B', 350000.00),
    ('SPA_ZONE_C', 'Khu Spa - Khu C', 'Khu spa với phòng xông hơi, massage và khu vực thư giãn. Dành cho tối đa 4 người.', 'Tầng 2, tòa C', 350000.00),
    ('SPA_ZONE_D', 'Khu Spa - Khu D', 'Khu spa với phòng xông hơi, massage và khu vực thư giãn. Dành cho tối đa 4 người.', 'Tầng 2, tòa D', 350000.00)
) AS t(code_val, name_val, desc_val, location_val, price_val)
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Khu vui chơi giải trí - 20 people per zone
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
    code_val,
    name_val,
    desc_val,
    location_val,
    'https://maps.example.com/' || LOWER(code_val),
    price_val,
    'HOURLY',
    20, -- Khu vui chơi: 20 people max per zone
    1, -- Min 1 hour
    5, -- Max 5 hours
    30,
    '1. Giữ gìn đồ chơi và thiết bị
2. Người lớn cần giám sát trẻ em
3. Không chạy nhảy trong khu vực nguy hiểm
4. Tuân thủ quy định an toàn
5. Số người tối đa: 20 người/khu',
    TRUE
FROM qhomebaseapp.service_category sc,
(VALUES
    ('PLAYGROUND_ZONE_A', 'Khu vui chơi giải trí - Khu A', 'Khu vui chơi rộng rãi với nhiều thiết bị giải trí cho trẻ em và gia đình. Đủ cho 20 người.', 'Khu vực sân chơi, tòa A', 150000.00),
    ('PLAYGROUND_ZONE_B', 'Khu vui chơi giải trí - Khu B', 'Khu vui chơi rộng rãi với nhiều thiết bị giải trí cho trẻ em và gia đình. Đủ cho 20 người.', 'Khu vực sân chơi, tòa B', 150000.00),
    ('PLAYGROUND_ZONE_C', 'Khu vui chơi giải trí - Khu C', 'Khu vui chơi rộng rãi với nhiều thiết bị giải trí cho trẻ em và gia đình. Đủ cho 20 người.', 'Khu vực sân chơi, tòa C', 150000.00),
    ('PLAYGROUND_ZONE_D', 'Khu vui chơi giải trí - Khu D', 'Khu vui chơi rộng rãi với nhiều thiết bị giải trí cho trẻ em và gia đình. Đủ cho 20 người.', 'Khu vực sân chơi, tòa D', 150000.00)
) AS t(code_val, name_val, desc_val, location_val, price_val)
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Bar Service - 30 people per zone
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
    code_val,
    name_val,
    desc_val,
    location_val,
    'https://maps.example.com/' || LOWER(code_val),
    price_val,
    'HOURLY',
    30, -- Bar: 30 people max per zone
    1, -- Min 1 hour
    6, -- Max 6 hours
    30,
    '1. Không hút thuốc trong khu vực
2. Giữ gìn vệ sinh chung
3. Tôn trọng các khách hàng khác
4. Không gây ồn ào quá mức
5. Số người tối đa: 30 người/khu',
    TRUE
FROM qhomebaseapp.service_category sc,
(VALUES
    ('BAR_ZONE_A', 'Bar - Khu A', 'Bar với không gian rộng rãi, âm nhạc và đồ uống. Dành cho tối đa 30 người.', 'Tầng 1, tòa A', 250000.00),
    ('BAR_ZONE_B', 'Bar - Khu B', 'Bar với không gian rộng rãi, âm nhạc và đồ uống. Dành cho tối đa 30 người.', 'Tầng 1, tòa B', 250000.00),
    ('BAR_ZONE_C', 'Bar - Khu C', 'Bar với không gian rộng rãi, âm nhạc và đồ uống. Dành cho tối đa 30 người.', 'Tầng 1, tòa C', 250000.00),
    ('BAR_ZONE_D', 'Bar - Khu D', 'Bar với không gian rộng rãi, âm nhạc và đồ uống. Dành cho tối đa 30 người.', 'Tầng 1, tòa D', 250000.00)
) AS t(code_val, name_val, desc_val, location_val, price_val)
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert availability schedules for all services
-- BBQ: Monday to Sunday, 9:00 - 21:00
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, day_num, '09:00:00'::TIME, '21:00:00'::TIME, TRUE
FROM qhomebaseapp.service s
CROSS JOIN generate_series(1, 7) AS day_num
WHERE s.code LIKE 'BBQ_ZONE_%' OR s.code LIKE 'SPA_ZONE_%' OR s.code LIKE 'PLAYGROUND_ZONE_%' OR s.code LIKE 'BAR_ZONE_%'
ON CONFLICT DO NOTHING;

