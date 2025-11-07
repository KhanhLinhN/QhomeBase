-- Update user_id cho resident Nguyễn Văn A
UPDATE data.residents
SET user_id = '550e8400-e29b-41d4-a716-446655440110'::uuid,
    updated_at = now()
WHERE id = '550e8400-e29b-41d4-a716-446655440100'::uuid;

-- Verify kết quả
SELECT id, full_name, email, user_id
FROM data.residents
WHERE id = '550e8400-e29b-41d4-a716-446655440100'::uuid;