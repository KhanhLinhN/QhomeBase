-- ============================================
-- SQL Queries để test Export Readings by Cycle
-- ============================================

-- 1. Tìm readings theo cycleId (giống query trong code)
-- Thay 'YOUR_CYCLE_ID' bằng cycleId thực tế
SELECT mr.id, mr.meter_id, mr.session_id, mr.reading_date, mr.curr_index, mr.prev_index
FROM data.meter_readings mr
WHERE (
    (mr.session_id IS NOT NULL 
     AND EXISTS (
         SELECT 1 FROM data.meter_reading_sessions mrs 
         WHERE mrs.id = mr.session_id 
         AND mrs.cycle_id IS NOT NULL 
         AND mrs.cycle_id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b'
     ))
    OR 
    (mr.session_id IS NOT NULL 
     AND EXISTS (
         SELECT 1 FROM data.meter_reading_sessions mrs 
         JOIN data.meter_reading_assignments mra ON mra.id = mrs.assignment_id
         WHERE mrs.id = mr.session_id 
         AND mra.cycle_id IS NOT NULL 
         AND mra.cycle_id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b'
     ))
);

-- 2. Kiểm tra tất cả readings và cycleId của chúng
SELECT 
    mr.id AS reading_id,
    mr.session_id,
    mrs.cycle_id AS session_cycle_id,
    mra.cycle_id AS assignment_cycle_id,
    CASE 
        WHEN mrs.cycle_id IS NOT NULL THEN mrs.cycle_id::text
        WHEN mra.cycle_id IS NOT NULL THEN mra.cycle_id::text
        ELSE 'NO CYCLE'
    END AS effective_cycle_id
FROM data.meter_readings mr
LEFT JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
LEFT JOIN data.meter_reading_assignments mra ON mra.id = mrs.assignment_id
ORDER BY mr.created_at DESC
LIMIT 20;

-- 3. Kiểm tra readings có session với cycleId cụ thể
SELECT 
    mr.id AS reading_id,
    mr.session_id,
    mrs.cycle_id,
    mrs.assignment_id,
    mra.cycle_id AS assignment_cycle_id
FROM data.meter_readings mr
LEFT JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
LEFT JOIN data.meter_reading_assignments mra ON mra.id = mrs.assignment_id
WHERE mr.session_id IS NOT NULL
ORDER BY mr.created_at DESC
LIMIT 10;

-- 4. Tìm tất cả cycleId có trong database
SELECT DISTINCT 
    mrs.cycle_id AS cycle_id_from_session,
    COUNT(*) AS readings_count
FROM data.meter_readings mr
INNER JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
WHERE mrs.cycle_id IS NOT NULL
GROUP BY mrs.cycle_id
ORDER BY readings_count DESC;

-- 5. Kiểm tra cycleId có tồn tại trong reading_cycles không
SELECT 
    rc.id,
    rc.name,
    rc.period_from,
    rc.period_to,
    rc.status
FROM data.reading_cycles rc
WHERE rc.id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b';

-- 6. Tìm readings theo cycleId - Phiên bản đơn giản hơn
SELECT 
    mr.id,
    mr.meter_id,
    mr.session_id,
    mrs.cycle_id,
    mr.reading_date,
    mr.curr_index,
    mr.prev_index
FROM data.meter_readings mr
INNER JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
WHERE mrs.cycle_id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b';

-- 7. Tìm readings qua assignment.cycle_id
SELECT 
    mr.id,
    mr.meter_id,
    mr.session_id,
    mrs.assignment_id,
    mra.cycle_id,
    mr.reading_date
FROM data.meter_readings mr
INNER JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
INNER JOIN data.meter_reading_assignments mra ON mra.id = mrs.assignment_id
WHERE mra.cycle_id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b';

-- 8. Debug: Xem chi tiết một reading cụ thể
-- Thay 'YOUR_READING_ID' bằng meterReadingId thực tế
SELECT 
    mr.id AS reading_id,
    mr.meter_id,
    mr.session_id,
    mr.reading_date,
    mr.curr_index,
    mr.prev_index,
    mrs.id AS session_id_check,
    mrs.cycle_id AS session_cycle_id,
    mrs.assignment_id,
    mra.id AS assignment_id_check,
    mra.cycle_id AS assignment_cycle_id
FROM data.meter_readings mr
LEFT JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
LEFT JOIN data.meter_reading_assignments mra ON mra.id = mrs.assignment_id
WHERE mr.id = 'fcbcf0fe-9c41-47a6-8bef-ab2051b5c96e';  -- Thay bằng reading ID thực tế

-- 9. Kiểm tra session có cycleId đúng không
SELECT 
    mrs.id AS session_id,
    mrs.cycle_id,
    mrs.assignment_id,
    mrs.reader_id,
    mrs.started_at
FROM data.meter_reading_sessions mrs
WHERE mrs.cycle_id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b';

-- 10. Tổng hợp: Đếm readings theo cycleId (cả session và assignment)
SELECT 
    COALESCE(mrs.cycle_id, mra.cycle_id) AS cycle_id,
    COUNT(mr.id) AS readings_count
FROM data.meter_readings mr
LEFT JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
LEFT JOIN data.meter_reading_assignments mra ON mra.id = mrs.assignment_id
WHERE COALESCE(mrs.cycle_id, mra.cycle_id) = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b'
GROUP BY COALESCE(mrs.cycle_id, mra.cycle_id);


