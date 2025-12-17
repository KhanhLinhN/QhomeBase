-- Script tạo 15 notifications mẫu
-- Tạo các notifications với các type, scope, và target khác nhau

DO $$
DECLARE
    admin_user_id UUID := COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    );
    building_a_id UUID := (SELECT id FROM data.buildings WHERE code = 'A' LIMIT 1);
    building_b_id UUID := (SELECT id FROM data.buildings WHERE code = 'B' LIMIT 1);
    building_c_id UUID := (SELECT id FROM data.buildings WHERE code = 'C' LIMIT 1);
    sample_resident_id UUID := (SELECT id FROM data.residents LIMIT 1);
    sample_contract_id UUID := (SELECT id FROM files.contracts LIMIT 1);
    sample_invoice_id UUID := (SELECT id FROM billing.invoices LIMIT 1);
BEGIN
    -- 1. Notification INFO - EXTERNAL - All buildings
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'INFO',
        'Thông báo về dịch vụ bảo trì',
        'Tòa nhà sẽ được bảo trì định kỳ vào tháng tới. Vui lòng chú ý các thông báo tiếp theo.',
        'EXTERNAL',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        '/base/news',
        'https://example.com/icons/info.png',
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days',
        NULL,
        NULL
    );

    -- 2. Notification BILL - EXTERNAL - Building A
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'BILL',
        'Hóa đơn mới đã được phát hành',
        'Hóa đơn tháng 12/2025 đã được phát hành. Vui lòng thanh toán trước ngày 25/12/2025.',
        'EXTERNAL',
        NULL,
        building_a_id,
        NULL,
        sample_invoice_id,
        'INVOICE',
        '/base/finance/invoices',
        'https://example.com/icons/bill.png',
        NOW() - INTERVAL '2 days',
        NOW() - INTERVAL '2 days',
        NULL,
        NULL
    );

    -- 3. Notification CONTRACT - EXTERNAL - Specific resident
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'CONTRACT',
        'Nhắc nhở gia hạn hợp đồng',
        'Hợp đồng của bạn sắp hết hạn trong 30 ngày. Vui lòng liên hệ ban quản lý để gia hạn.',
        'EXTERNAL',
        NULL,
        NULL,
        sample_resident_id,
        sample_contract_id,
        'CONTRACT',
        '/base/contract/rental-review',
        'https://example.com/icons/contract.png',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day',
        NULL,
        NULL
    );

    -- 4. Notification ELECTRICITY - EXTERNAL - Building B
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'ELECTRICITY',
        'Thông báo về hóa đơn điện',
        'Hóa đơn điện tháng 12/2025 đã được phát hành. Số tiền: 500,000 VNĐ.',
        'EXTERNAL',
        NULL,
        building_b_id,
        NULL,
        sample_invoice_id,
        'INVOICE',
        '/base/finance/invoices',
        'https://example.com/icons/electricity.png',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days',
        NULL,
        NULL
    );

    -- 5. Notification WATER - EXTERNAL - Building C
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'WATER',
        'Thông báo về hóa đơn nước',
        'Hóa đơn nước tháng 12/2025 đã được phát hành. Số tiền: 300,000 VNĐ.',
        'EXTERNAL',
        NULL,
        building_c_id,
        NULL,
        sample_invoice_id,
        'INVOICE',
        '/base/finance/invoices',
        'https://example.com/icons/water.png',
        NOW() - INTERVAL '4 days',
        NOW() - INTERVAL '4 days',
        NULL,
        NULL
    );

    -- 6. Notification SYSTEM - INTERNAL - ALL roles
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'SYSTEM',
        'Hệ thống đã được cập nhật',
        'Hệ thống quản lý đã được cập nhật lên phiên bản mới. Vui lòng đăng nhập lại để sử dụng các tính năng mới.',
        'INTERNAL',
        'ALL',
        NULL,
        NULL,
        NULL,
        NULL,
        '/base/dashboard',
        'https://example.com/icons/system.png',
        NOW() - INTERVAL '10 days',
        NOW() - INTERVAL '10 days',
        NULL,
        NULL
    );

    -- 7. Notification REQUEST - EXTERNAL - All buildings
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'REQUEST',
        'Yêu cầu của bạn đã được xử lý',
        'Yêu cầu sửa chữa của bạn đã được xử lý. Vui lòng kiểm tra kết quả.',
        'EXTERNAL',
        NULL,
        NULL,
        NULL,
        gen_random_uuid(),
        'SERVICE_REQUEST',
        '/base/services',
        'https://example.com/icons/request.png',
        NOW() - INTERVAL '6 days',
        NOW() - INTERVAL '6 days',
        NULL,
        NULL
    );

    -- 8. Notification SYSTEM - INTERNAL - ADMIN only
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'SYSTEM',
        'Báo cáo hàng ngày',
        'Báo cáo hoạt động hàng ngày đã được tạo. Vui lòng xem xét.',
        'INTERNAL',
        'ADMIN',
        NULL,
        NULL,
        NULL,
        NULL,
        '/base/reports',
        'https://example.com/icons/report.png',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day',
        NULL,
        NULL
    );

    -- 9. Notification CARD_APPROVED - EXTERNAL - Specific resident
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'CARD_APPROVED',
        'Thẻ của bạn đã được phê duyệt',
        'Đơn đăng ký thẻ của bạn đã được phê duyệt. Vui lòng đến ban quản lý để nhận thẻ.',
        'EXTERNAL',
        NULL,
        NULL,
        sample_resident_id,
        gen_random_uuid(),
        'CARD_REGISTRATION',
        '/base/cards',
        'https://example.com/icons/card-approved.png',
        NOW() - INTERVAL '7 days',
        NOW() - INTERVAL '7 days',
        NULL,
        NULL
    );

    -- 10. Notification CARD_REJECTED - EXTERNAL - Specific resident
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'CARD_REJECTED',
        'Đơn đăng ký thẻ của bạn đã bị từ chối',
        'Đơn đăng ký thẻ của bạn đã bị từ chối. Vui lòng liên hệ ban quản lý để biết thêm chi tiết.',
        'EXTERNAL',
        NULL,
        NULL,
        sample_resident_id,
        gen_random_uuid(),
        'CARD_REGISTRATION',
        '/base/cards',
        'https://example.com/icons/card-rejected.png',
        NOW() - INTERVAL '8 days',
        NOW() - INTERVAL '8 days',
        NULL,
        NULL
    );

    -- 11. Notification CARD_FEE_REMINDER - EXTERNAL - All buildings
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'CARD_FEE_REMINDER',
        'Nhắc nhở phí thẻ',
        'Phí thẻ hàng tháng của bạn đã đến hạn thanh toán. Vui lòng thanh toán trước ngày 25/12/2025.',
        'EXTERNAL',
        NULL,
        NULL,
        NULL,
        gen_random_uuid(),
        'CARD_FEE',
        '/base/finance/invoices',
        'https://example.com/icons/card-fee.png',
        NOW() - INTERVAL '2 days',
        NOW() - INTERVAL '2 days',
        NULL,
        NULL
    );

    -- 12. Notification SYSTEM - INTERNAL - TECHNICIAN only
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'SYSTEM',
        'Công việc mới được phân công',
        'Bạn có một công việc bảo trì mới được phân công. Vui lòng kiểm tra và xử lý.',
        'INTERNAL',
        'TECHNICIAN',
        NULL,
        NULL,
        gen_random_uuid(),
        'MAINTENANCE_TASK',
        '/base/maintenance',
        'https://example.com/icons/task.png',
        NOW() - INTERVAL '3 days',
        NOW() - INTERVAL '3 days',
        NULL,
        NULL
    );

    -- 13. Notification BILL - EXTERNAL - All buildings (soft deleted)
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'BILL',
        'Hóa đơn đã bị xóa',
        'Hóa đơn này đã bị xóa.',
        'EXTERNAL',
        NULL,
        NULL,
        NULL,
        sample_invoice_id,
        'INVOICE',
        '/base/finance/invoices',
        'https://example.com/icons/bill.png',
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '20 days',
        NOW() - INTERVAL '5 days',
        admin_user_id
    );

    -- 14. Notification INFO - EXTERNAL - Building A
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'INFO',
        'Thông báo về sự kiện tòa nhà A',
        'Sự kiện giao lưu cư dân tòa nhà A sẽ được tổ chức vào cuối tuần này.',
        'EXTERNAL',
        NULL,
        building_a_id,
        NULL,
        NULL,
        NULL,
        '/base/events',
        'https://example.com/icons/event.png',
        NOW() - INTERVAL '1 day',
        NOW() - INTERVAL '1 day',
        NULL,
        NULL
    );

    -- 15. Notification SYSTEM - INTERNAL - ACCOUNTANT only
    INSERT INTO content.notifications (
        id, type, title, message, scope, target_role, target_building_id, target_resident_id,
        reference_id, reference_type, action_url, icon_url,
        created_at, updated_at, deleted_at, deleted_by
    ) VALUES (
        gen_random_uuid(),
        'SYSTEM',
        'Nhắc nhở báo cáo tài chính',
        'Báo cáo tài chính tháng 12/2025 cần được hoàn thành trước ngày 31/12/2025.',
        'INTERNAL',
        'ACCOUNTANT',
        NULL,
        NULL,
        NULL,
        NULL,
        '/base/finance/reports',
        'https://example.com/icons/finance-report.png',
        NOW() - INTERVAL '5 days',
        NOW() - INTERVAL '5 days',
        NULL,
        NULL
    );

END $$;

-- Hiển thị kết quả
SELECT 
    id,
    type,
    title,
    scope,
    target_role,
    target_building_id,
    target_resident_id,
    reference_type,
    deleted_at,
    created_at
FROM content.notifications
WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 15;

