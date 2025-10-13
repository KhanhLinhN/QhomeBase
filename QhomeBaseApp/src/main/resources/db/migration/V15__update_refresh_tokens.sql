-- V15__update_refresh_tokens.sql

-- Thêm cột device_id nếu chưa tồn tại
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'qhomebaseapp'
          AND table_name = 'refresh_tokens'
          AND column_name = 'device_id'
    ) THEN
        ALTER TABLE qhomebaseapp.refresh_tokens
        ADD COLUMN device_id VARCHAR(255) NOT NULL DEFAULT 'default';
    END IF;
END$$;

-- Tạo index hoặc unique constraint cho device_id + user_id nếu cần
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
        WHERE tc.table_schema = 'qhomebaseapp'
          AND tc.table_name = 'refresh_tokens'
          AND tc.constraint_type = 'UNIQUE'
          AND kcu.column_name = 'device_id'
    ) THEN
        ALTER TABLE qhomebaseapp.refresh_tokens
        ADD CONSTRAINT uq_refresh_token_user_device UNIQUE(user_id, device_id);
    END IF;
END$$;
