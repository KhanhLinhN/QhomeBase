-- Drop legacy columns if they still exist after previous migrations
ALTER TABLE cs_service.requests
    DROP COLUMN IF EXISTS priority;

ALTER TABLE cs_service.processing_logs
    DROP COLUMN IF EXISTS record_type;

ALTER TABLE cs_service.processing_logs
    DROP COLUMN IF EXISTS log_type;

-- Rename request_status enum values to match new workflow (Pending → Processing → Done)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum e
                 JOIN pg_type t ON e.enumtypid = t.oid
                 JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'cs_service'
          AND t.typname = 'request_status'
          AND e.enumlabel = 'New'
    ) THEN
        EXECUTE 'ALTER TYPE cs_service.request_status RENAME VALUE ''New'' TO ''Pending''';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_enum e
                 JOIN pg_type t ON e.enumtypid = t.oid
                 JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'cs_service'
          AND t.typname = 'request_status'
          AND e.enumlabel = 'Completed'
    ) THEN
        EXECUTE 'ALTER TYPE cs_service.request_status RENAME VALUE ''Completed'' TO ''Done''';
    END IF;
END
$$;

-- Reset sample data
DELETE FROM cs_service.processing_logs;
DELETE FROM cs_service.requests;

-- Insert sample Pending request
WITH req AS (
    INSERT INTO cs_service.requests (id, request_code, resident_id, resident_name, image_path, title, content, status, created_at, updated_at)
    VALUES (
            gen_random_uuid(),
            'REQ-2025-00001',
            gen_random_uuid(),
            'Nguyễn Văn A',
            NULL,
            'Yêu cầu sửa máy lạnh',
            'Máy lạnh phòng khách kêu to, cần kiểm tra.',
            'Pending',
            NOW() - INTERVAL '2 days',
            NOW() - INTERVAL '2 days'
        )
    RETURNING id, resident_name, created_at
)
INSERT INTO cs_service.processing_logs (id, record_id, staff_in_charge, content, request_status, staff_in_charge_name, created_at)
SELECT
    gen_random_uuid(),
    req.id,
    NULL,
    'Cư dân tạo yêu cầu mới.',
    'Pending',
    req.resident_name,
    req.created_at
FROM req;

-- Insert sample Processing request
WITH req AS (
    INSERT INTO cs_service.requests (id, request_code, resident_id, resident_name, image_path, title, content, status, created_at, updated_at)
    VALUES (
            gen_random_uuid(),
            'REQ-2025-00002',
            gen_random_uuid(),
            'Trần Thị B',
            NULL,
            'Sự cố mất nước tầng 5',
            'Cả tầng 5 đang bị mất nước từ sáng nay.',
            'Processing',
            NOW() - INTERVAL '1 day',
            NOW() - INTERVAL '12 hours'
        )
    RETURNING id, resident_name, created_at
)
INSERT INTO cs_service.processing_logs (id, record_id, staff_in_charge, content, request_status, staff_in_charge_name, created_at)
SELECT gen_random_uuid(), req.id, NULL, 'Cư dân báo mất nước.', 'Pending', req.resident_name, req.created_at
FROM req
UNION ALL
SELECT gen_random_uuid(), req.id, gen_random_uuid(), 'Nhân viên kỹ thuật đã tiếp nhận và đang xử lý.', 'Processing', 'Nguyễn Kỹ Thuật', req.created_at + INTERVAL '1 hour'
FROM req;

-- Insert sample Done request
WITH req AS (
    INSERT INTO cs_service.requests (id, request_code, resident_id, resident_name, image_path, title, content, status, created_at, updated_at)
    VALUES (
            gen_random_uuid(),
            'REQ-2025-00003',
            gen_random_uuid(),
            'Lê Quốc C',
            NULL,
            'Đăng ký vệ sinh hồ bơi',
            'Đề nghị vệ sinh hồ bơi khu A trước cuối tuần.',
            'Done',
            NOW() - INTERVAL '3 days',
            NOW() - INTERVAL '6 hours'
        )
    RETURNING id, resident_name, created_at
)
INSERT INTO cs_service.processing_logs (id, record_id, staff_in_charge, content, request_status, staff_in_charge_name, created_at)
SELECT gen_random_uuid(), req.id, NULL, 'Cư dân gửi yêu cầu.', 'Pending', req.resident_name, req.created_at
FROM req
UNION ALL
SELECT gen_random_uuid(), req.id, gen_random_uuid(), 'Đã phân công đội vệ sinh thực hiện.', 'Processing', 'Phạm Hỗ Trợ', req.created_at + INTERVAL '4 hours'
FROM req
UNION ALL
SELECT gen_random_uuid(), req.id, gen_random_uuid(), 'Đã hoàn tất vệ sinh hồ bơi.', 'Done', 'Phạm Hỗ Trợ', req.created_at + INTERVAL '2 days'
FROM req;

