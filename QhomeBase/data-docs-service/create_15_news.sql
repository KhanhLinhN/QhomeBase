-- Script tạo 15 news mẫu
-- Tạo các news với các status, scope, và target khác nhau

DO $$
DECLARE
    admin_user_id UUID := COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    );
    building_a_id UUID := (SELECT id FROM data.buildings WHERE code = 'A' LIMIT 1);
    building_b_id UUID := (SELECT id FROM data.buildings WHERE code = 'B' LIMIT 1);
    building_c_id UUID := (SELECT id FROM data.buildings WHERE code = 'C' LIMIT 1);
BEGIN
    -- 1. News PUBLISHED - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về dịch vụ bảo trì tòa nhà',
        'Tòa nhà sẽ được bảo trì định kỳ vào tháng tới. Vui lòng chú ý các thông báo tiếp theo.',
        '<p>Tòa nhà sẽ được bảo trì định kỳ vào tháng tới. Vui lòng chú ý các thông báo tiếp theo.</p><p>Thời gian: Từ 8:00 - 17:00</p><p>Mọi thắc mắc xin liên hệ ban quản lý.</p>',
        'https://example.com/images/maintenance.jpg',
        'PUBLISHED',
        NOW() - INTERVAL '5 days',
        NOW() + INTERVAL '30 days',
        1,
        125,
        NOW() - INTERVAL '5 days',
        admin_user_id,
        NOW() - INTERVAL '5 days',
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

    -- 2. News PUBLISHED - EXTERNAL - Building A only
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về hệ thống thang máy tòa nhà A',
        'Hệ thống thang máy tòa nhà A sẽ được nâng cấp vào tuần tới.',
        '<p>Hệ thống thang máy tòa nhà A sẽ được nâng cấp vào tuần tới.</p><p>Thời gian: 7:00 - 18:00</p><p>Xin lỗi vì sự bất tiện này.</p>',
        'https://example.com/images/elevator.jpg',
        'PUBLISHED',
        NOW() - INTERVAL '2 days',
        NOW() + INTERVAL '15 days',
        2,
        89,
        NOW() - INTERVAL '2 days',
        admin_user_id,
        NOW() - INTERVAL '2 days',
        admin_user_id,
        'EXTERNAL',
        NULL,
        building_a_id
    );

    -- 3. News PUBLISHED - INTERNAL - ALL roles
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Hướng dẫn sử dụng hệ thống quản lý mới',
        'Hệ thống quản lý đã được cập nhật với nhiều tính năng mới.',
        '<p>Hệ thống quản lý đã được cập nhật với nhiều tính năng mới.</p><p>Vui lòng đọc tài liệu hướng dẫn để nắm rõ các thay đổi.</p>',
        'https://example.com/images/system-guide.jpg',
        'PUBLISHED',
        NOW() - INTERVAL '10 days',
        NULL,
        3,
        234,
        NOW() - INTERVAL '10 days',
        admin_user_id,
        NOW() - INTERVAL '10 days',
        admin_user_id,
        'INTERNAL',
        'ALL',
        NULL
    );

    -- 4. News SCHEDULED - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về chương trình khuyến mãi tháng 12',
        'Chương trình khuyến mãi đặc biệt dành cho cư dân trong tháng 12.',
        '<p>Chương trình khuyến mãi đặc biệt dành cho cư dân trong tháng 12.</p><p>Giảm 20% phí dịch vụ cho các hóa đơn thanh toán trước ngày 25/12.</p>',
        'https://example.com/images/promotion.jpg',
        'SCHEDULED',
        NOW() + INTERVAL '3 days',
        NOW() + INTERVAL '30 days',
        4,
        0,
        NOW(),
        admin_user_id,
        NOW(),
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

    -- 5. News PUBLISHED - INTERNAL - ADMIN only
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Báo cáo tài chính tháng 11',
        'Báo cáo tài chính tháng 11 đã được cập nhật trong hệ thống.',
        '<p>Báo cáo tài chính tháng 11 đã được cập nhật trong hệ thống.</p><p>Vui lòng xem xét và phê duyệt.</p>',
        NULL,
        'PUBLISHED',
        NOW() - INTERVAL '1 day',
        NULL,
        5,
        12,
        NOW() - INTERVAL '1 day',
        admin_user_id,
        NOW() - INTERVAL '1 day',
        admin_user_id,
        'INTERNAL',
        'ADMIN',
        NULL
    );

    -- 6. News PUBLISHED - EXTERNAL - Building B only
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về việc sửa chữa hệ thống nước tòa nhà B',
        'Hệ thống nước tòa nhà B sẽ được sửa chữa vào ngày mai.',
        '<p>Hệ thống nước tòa nhà B sẽ được sửa chữa vào ngày mai.</p><p>Thời gian: 9:00 - 16:00</p><p>Nước sẽ bị ngắt trong thời gian này.</p>',
        'https://example.com/images/water-repair.jpg',
        'PUBLISHED',
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '2 days',
        6,
        156,
        NOW() - INTERVAL '1 day',
        admin_user_id,
        NOW() - INTERVAL '1 day',
        admin_user_id,
        'EXTERNAL',
        NULL,
        building_b_id
    );

    -- 7. News DRAFT - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về sự kiện năm mới (Draft)',
        'Sự kiện chào mừng năm mới sẽ được tổ chức vào cuối tháng 12.',
        '<p>Sự kiện chào mừng năm mới sẽ được tổ chức vào cuối tháng 12.</p><p>Thời gian và địa điểm sẽ được thông báo sau.</p>',
        'https://example.com/images/new-year.jpg',
        'DRAFT',
        NULL,
        NULL,
        7,
        0,
        NOW(),
        admin_user_id,
        NOW(),
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

    -- 8. News PUBLISHED - INTERNAL - TECHNICIAN only
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Hướng dẫn bảo trì thiết bị mới',
        'Các thiết bị bảo trì mới đã được cập nhật. Vui lòng xem hướng dẫn sử dụng.',
        '<p>Các thiết bị bảo trì mới đã được cập nhật.</p><p>Vui lòng xem hướng dẫn sử dụng trong tài liệu đính kèm.</p>',
        NULL,
        'PUBLISHED',
        NOW() - INTERVAL '7 days',
        NULL,
        8,
        45,
        NOW() - INTERVAL '7 days',
        admin_user_id,
        NOW() - INTERVAL '7 days',
        admin_user_id,
        'INTERNAL',
        'TECHNICIAN',
        NULL
    );

    -- 9. News EXPIRED - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về sự kiện đã kết thúc',
        'Sự kiện này đã kết thúc.',
        '<p>Sự kiện này đã kết thúc.</p>',
        'https://example.com/images/event-ended.jpg',
        'EXPIRED',
        NOW() - INTERVAL '60 days',
        NOW() - INTERVAL '30 days',
        9,
        567,
        NOW() - INTERVAL '60 days',
        admin_user_id,
        NOW() - INTERVAL '30 days',
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

    -- 10. News PUBLISHED - EXTERNAL - Building C only
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về việc lắp đặt camera an ninh tòa nhà C',
        'Hệ thống camera an ninh mới sẽ được lắp đặt tại tòa nhà C.',
        '<p>Hệ thống camera an ninh mới sẽ được lắp đặt tại tòa nhà C.</p><p>Thời gian: Tuần tới</p><p>Mọi thắc mắc xin liên hệ ban quản lý.</p>',
        'https://example.com/images/security-camera.jpg',
        'PUBLISHED',
        NOW() - INTERVAL '3 days',
        NOW() + INTERVAL '20 days',
        10,
        201,
        NOW() - INTERVAL '3 days',
        admin_user_id,
        NOW() - INTERVAL '3 days',
        admin_user_id,
        'EXTERNAL',
        NULL,
        building_c_id
    );

    -- 11. News SCHEDULED - INTERNAL - ACCOUNTANT only
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Họp bàn về quy trình thanh toán mới',
        'Cuộc họp về quy trình thanh toán mới sẽ được tổ chức vào tuần tới.',
        '<p>Cuộc họp về quy trình thanh toán mới sẽ được tổ chức vào tuần tới.</p><p>Thời gian: 14:00 - 16:00</p><p>Địa điểm: Phòng họp tầng 1</p>',
        NULL,
        'SCHEDULED',
        NOW() + INTERVAL '5 days',
        NULL,
        11,
        0,
        NOW(),
        admin_user_id,
        NOW(),
        admin_user_id,
        'INTERNAL',
        'ACCOUNTANT',
        NULL
    );

    -- 12. News PUBLISHED - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo về dịch vụ vệ sinh tòa nhà',
        'Dịch vụ vệ sinh tòa nhà sẽ được thực hiện vào thứ 7 hàng tuần.',
        '<p>Dịch vụ vệ sinh tòa nhà sẽ được thực hiện vào thứ 7 hàng tuần.</p><p>Thời gian: 8:00 - 12:00</p><p>Vui lòng dọn dẹp đồ đạc trước giờ vệ sinh.</p>',
        'https://example.com/images/cleaning.jpg',
        'PUBLISHED',
        NOW() - INTERVAL '15 days',
        NULL,
        12,
        342,
        NOW() - INTERVAL '15 days',
        admin_user_id,
        NOW() - INTERVAL '15 days',
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

    -- 13. News HIDDEN - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo đã bị ẩn',
        'Thông báo này đã bị ẩn khỏi danh sách.',
        '<p>Thông báo này đã bị ẩn khỏi danh sách.</p>',
        NULL,
        'HIDDEN',
        NOW() - INTERVAL '20 days',
        NULL,
        13,
        89,
        NOW() - INTERVAL '20 days',
        admin_user_id,
        NOW() - INTERVAL '1 day',
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

    -- 14. News PUBLISHED - INTERNAL - ALL roles
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Cập nhật chính sách làm việc từ xa',
        'Chính sách làm việc từ xa đã được cập nhật. Vui lòng xem chi tiết.',
        '<p>Chính sách làm việc từ xa đã được cập nhật.</p><p>Vui lòng xem chi tiết trong tài liệu đính kèm.</p>',
        NULL,
        'PUBLISHED',
        NOW() - INTERVAL '4 days',
        NULL,
        14,
        78,
        NOW() - INTERVAL '4 days',
        admin_user_id,
        NOW() - INTERVAL '4 days',
        admin_user_id,
        'INTERNAL',
        'ALL',
        NULL
    );

    -- 15. News ARCHIVED - EXTERNAL - All buildings
    INSERT INTO content.news (
        id, title, summary, body_html, cover_image_url, status, publish_at, expire_at,
        display_order, view_count, created_at, created_by, updated_at, updated_by,
        scope, target_role, target_building_id
    ) VALUES (
        gen_random_uuid(),
        'Thông báo đã được lưu trữ',
        'Thông báo này đã được lưu trữ.',
        '<p>Thông báo này đã được lưu trữ.</p>',
        NULL,
        'ARCHIVED',
        NOW() - INTERVAL '90 days',
        NOW() - INTERVAL '60 days',
        15,
        1234,
        NOW() - INTERVAL '90 days',
        admin_user_id,
        NOW() - INTERVAL '60 days',
        admin_user_id,
        'EXTERNAL',
        NULL,
        NULL
    );

END $$;

-- Hiển thị kết quả
SELECT 
    id,
    title,
    status,
    scope,
    target_role,
    target_building_id,
    publish_at,
    expire_at,
    view_count,
    created_at
FROM content.news
ORDER BY created_at DESC
LIMIT 15;

