-- Flyway migration V35
-- Insert test data for Pool and Playground services under Entertainment category

-- Insert Pool services (Hồ bơi)
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
    'POOL_ZONE_A',
    'Hồ bơi - Tòa A',
    'Hồ bơi rộng 25m x 12m, sâu 1.2m - 1.8m. Có mái che, khu vực nghỉ ngơi, phòng thay đồ và vòi sen.',
    'Tòa nhà A, tầng 5, khu vực hồ bơi',
    'https://maps.example.com/pool-zone-a',
    150000.00, -- 150,000 VNĐ/giờ
    'HOURLY',
    20, -- Tối đa 20 người
    1, -- Tối thiểu 1 giờ
    4, -- Tối đa 4 giờ
    30,
    '1. Không được bơi khi không có nhân viên cứu hộ
2. Trẻ em dưới 12 tuổi phải có người lớn đi kèm
3. Mặc đồ bơi phù hợp
4. Không được mang đồ ăn uống vào khu vực hồ bơi
5. Tuân thủ quy định về số lượng người tối đa',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

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
    'POOL_ZONE_B',
    'Hồ bơi - Tòa B',
    'Hồ bơi rộng 20m x 10m, sâu 0.8m - 1.5m. Phù hợp cho trẻ em và người mới học bơi.',
    'Tòa nhà B, tầng 5, khu vực hồ bơi',
    'https://maps.example.com/pool-zone-b',
    120000.00, -- 120,000 VNĐ/giờ
    'HOURLY',
    15, -- Tối đa 15 người
    1,
    4,
    30,
    '1. Không được bơi khi không có nhân viên cứu hộ
2. Trẻ em dưới 12 tuổi phải có người lớn đi kèm
3. Mặc đồ bơi phù hợp
4. Không được mang đồ ăn uống vào khu vực hồ bơi
5. Tuân thủ quy định về số lượng người tối đa',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert Playground services (Sân chơi)
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
    'PLAYGROUND_ZONE_A',
    'Sân chơi - Tòa A',
    'Sân chơi rộng rãi với cầu trượt, xích đu, bập bênh, khu vực cát chơi. Có mái che và ghế ngồi cho phụ huynh.',
    'Tòa nhà A, tầng trệt, khu vực sân chơi',
    'https://maps.example.com/playground-zone-a',
    50000.00, -- 50,000 VNĐ/giờ
    'HOURLY',
    30, -- Tối đa 30 trẻ em
    1,
    3,
    30,
    '1. Phụ huynh phải giám sát trẻ em trong suốt thời gian sử dụng
2. Không được leo trèo lên các thiết bị không an toàn
3. Giữ vệ sinh chung, không xả rác
4. Không sử dụng khi trời mưa hoặc thiết bị ướt
5. Tôn trọng quy định về số lượng trẻ tối đa',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

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
    'PLAYGROUND_ZONE_B',
    'Sân chơi - Tòa B',
    'Sân chơi ngoài trời với thiết bị thể thao, khu vực đá bóng mini, bóng rổ. Phù hợp cho trẻ lớn và thanh thiếu niên.',
    'Tòa nhà B, sân thượng, khu vực thể thao',
    'https://maps.example.com/playground-zone-b',
    80000.00, -- 80,000 VNĐ/giờ
    'HOURLY',
    25, -- Tối đa 25 người
    1,
    4,
    30,
    '1. Tuân thủ quy định an toàn khi chơi thể thao
2. Không được sử dụng thiết bị khi bị hỏng
3. Giữ vệ sinh chung
4. Tôn trọng quy định về số lượng người tối đa
5. Không gây ồn ào sau 21h',
    TRUE
FROM qhomebaseapp.service_category sc
WHERE sc.code = 'ENTERTAINMENT'
ON CONFLICT (code) DO NOTHING;

-- Insert Service Availability for Pool Zone A
-- Monday to Sunday: 6:00 - 20:00 (6 AM - 8 PM)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
UNION ALL
SELECT s.id, 2, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
UNION ALL
SELECT s.id, 3, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
UNION ALL
SELECT s.id, 4, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
UNION ALL
SELECT s.id, 5, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
UNION ALL
SELECT s.id, 6, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
UNION ALL
SELECT s.id, 0, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert Service Availability for Pool Zone B (same schedule)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
UNION ALL
SELECT s.id, 2, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
UNION ALL
SELECT s.id, 3, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
UNION ALL
SELECT s.id, 4, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
UNION ALL
SELECT s.id, 5, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
UNION ALL
SELECT s.id, 6, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
UNION ALL
SELECT s.id, 0, '06:00:00'::TIME, '20:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'POOL_ZONE_B'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert Service Availability for Playground Zone A
-- Monday to Sunday: 8:00 - 19:00 (8 AM - 7 PM)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
UNION ALL
SELECT s.id, 2, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
UNION ALL
SELECT s.id, 3, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
UNION ALL
SELECT s.id, 4, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
UNION ALL
SELECT s.id, 5, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
UNION ALL
SELECT s.id, 6, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
UNION ALL
SELECT s.id, 0, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_A'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

-- Insert Service Availability for Playground Zone B (same schedule)
INSERT INTO qhomebaseapp.service_availability(service_id, day_of_week, start_time, end_time, is_available)
SELECT s.id, 1, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
UNION ALL
SELECT s.id, 2, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
UNION ALL
SELECT s.id, 3, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
UNION ALL
SELECT s.id, 4, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
UNION ALL
SELECT s.id, 5, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
UNION ALL
SELECT s.id, 6, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
UNION ALL
SELECT s.id, 0, '08:00:00'::TIME, '19:00:00'::TIME, TRUE FROM qhomebaseapp.service s WHERE s.code = 'PLAYGROUND_ZONE_B'
ON CONFLICT (service_id, day_of_week, start_time, end_time) DO NOTHING;

