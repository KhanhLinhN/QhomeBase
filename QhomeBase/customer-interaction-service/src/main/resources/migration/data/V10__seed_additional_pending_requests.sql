-- Add extra sample Pending requests for testing

WITH req AS (
    INSERT INTO cs_service.requests (id, request_code, resident_id, resident_name, image_path, title, content, status, created_at, updated_at)
    VALUES
        (
            gen_random_uuid(),
            'REQ-2025-00004',
            gen_random_uuid(),
            'Phạm Minh D',
            NULL,
            'Đăng ký bảo trì thang máy',
            'Thang máy block B có tiếng kêu bất thường, đề nghị kiểm tra.',
            'Pending',
            NOW() - INTERVAL '12 hours',
            NOW() - INTERVAL '12 hours'
        ),
        (
            gen_random_uuid(),
            'REQ-2025-00005',
            gen_random_uuid(),
            'Đặng Thu E',
            NULL,
            'Yêu cầu tháo dỡ cây nghiêng',
            'Cây xanh trước nhà nghiêng vào đường, cần xử lý trước khi mưa bão.',
            'Pending',
            NOW() - INTERVAL '8 hours',
            NOW() - INTERVAL '8 hours'
        ),
        (
            gen_random_uuid(),
            'REQ-2025-00006',
            gen_random_uuid(),
            'Võ Quốc F',
            NULL,
            'Đề nghị thay bóng đèn hành lang',
            'Hành lang tầng 12 khu C có 3 bóng đèn bị cháy, đề nghị thay mới.',
            'Pending',
            NOW() - INTERVAL '4 hours',
            NOW() - INTERVAL '4 hours'
        )
    RETURNING id, resident_name, created_at
)
INSERT INTO cs_service.processing_logs (id, record_id, staff_in_charge, content, request_status, staff_in_charge_name, created_at)
SELECT gen_random_uuid(), req.id, NULL, 'Cư dân tạo yêu cầu.', 'Pending', req.resident_name, req.created_at
FROM req;

