-- Script để kiểm tra và cập nhật inspector_id trong database

-- 1. Kiểm tra xem cột inspector_id đã tồn tại chưa
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'data' 
  AND table_name = 'asset_inspections'
  AND column_name = 'inspector_id';

-- 2. Kiểm tra số lượng inspections có inspector_id null
SELECT COUNT(*) as null_count
FROM data.asset_inspections
WHERE inspector_id IS NULL;

-- 3. Xem các inspections có inspector_id null
SELECT 
    id,
    contract_id,
    unit_id,
    inspector_name,
    inspector_id,
    status,
    created_at
FROM data.asset_inspections
WHERE inspector_id IS NULL
ORDER BY created_at DESC;

-- 4. Nếu cần update inspections cũ (chỉ làm nếu có cách map inspector_name -> user_id)
-- UPDATE data.asset_inspections
-- SET inspector_id = (SELECT id FROM iam.users WHERE username = inspector_name OR email = inspector_name LIMIT 1)
-- WHERE inspector_id IS NULL AND inspector_name IS NOT NULL;

