-- ============================================
-- SQL Query đơn giản để test Export Readings by Cycle
-- (Giống query số 2 - đã có kết quả)
-- ============================================

-- ✅ Query chính: Tìm readings theo cycleId qua session
-- ⚠️ QUAN TRỌNG: Dùng cycle_id đúng từ readings!
-- Readings hiện tại có cycle_id = '728a8689-16de-4bad-a9a2-fb97a97cf043'
SELECT 
    mr.id,
    mr.meter_id,
    mr.session_id,
    mrs.cycle_id,
    mr.reading_date,
    mr.curr_index,
    mr.prev_index,
    mr.consumption,
    mr.note
FROM data.meter_readings mr
INNER JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
WHERE mrs.cycle_id = '728a8689-16de-4bad-a9a2-fb97a97cf043'  -- ✅ Cycle ID đúng từ readings
ORDER BY mr.reading_date DESC, mr.created_at DESC;

-- Hoặc test với cycle_id khác (nếu có):
-- WHERE mrs.cycle_id = '4534b90d-95f2-4582-8bb9-a5e22bcf8a6b'  -- Cycle ID khác (sẽ không có kết quả)

-- Query để đếm số readings
SELECT COUNT(*) AS total_readings
FROM data.meter_readings mr
INNER JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
WHERE mrs.cycle_id = '728a8689-16de-4bad-a9a2-fb97a97cf043';  -- ✅ Cycle ID đúng

-- Query để kiểm tra có bao nhiêu readings không có session
SELECT COUNT(*) AS readings_without_session
FROM data.meter_readings mr
WHERE mr.session_id IS NULL;

-- Query để kiểm tra có bao nhiêu sessions có cycle_id này
SELECT COUNT(*) AS sessions_count
FROM data.meter_reading_sessions mrs
WHERE mrs.cycle_id = '728a8689-16de-4bad-a9a2-fb97a97cf043';  -- ✅ Cycle ID đúng

-- Query để xem tất cả sessions và cycle_id
SELECT 
    mrs.id AS session_id,
    mrs.cycle_id,
    mrs.assignment_id,
    mrs.reader_id,
    mrs.started_at,
    COUNT(mr.id) AS readings_count
FROM data.meter_reading_sessions mrs
LEFT JOIN data.meter_readings mr ON mr.session_id = mrs.id
WHERE mrs.cycle_id = '728a8689-16de-4bad-a9a2-fb97a97cf043'  -- ✅ Cycle ID đúng
GROUP BY mrs.id, mrs.cycle_id, mrs.assignment_id, mrs.reader_id, mrs.started_at
ORDER BY mrs.started_at DESC;

-- Query để debug: Xem tất cả readings và session cycle_id của chúng
SELECT 
    mr.id AS reading_id,
    mr.session_id,
    mrs.cycle_id AS session_cycle_id,
    mr.reading_date,
    CASE 
        WHEN mrs.cycle_id = '728a8689-16de-4bad-a9a2-fb97a97cf043' THEN '✅ MATCH'
        ELSE '❌ NOT MATCH'
    END AS match_status
FROM data.meter_readings mr
LEFT JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
WHERE mr.session_id IS NOT NULL
ORDER BY mr.created_at DESC
LIMIT 20;

-- Query để tìm tất cả cycle_id có trong readings (quan trọng!)
SELECT DISTINCT 
    mrs.cycle_id,
    COUNT(mr.id) AS readings_count,
    MIN(mr.reading_date) AS earliest_reading,
    MAX(mr.reading_date) AS latest_reading
FROM data.meter_readings mr
INNER JOIN data.meter_reading_sessions mrs ON mrs.id = mr.session_id
WHERE mrs.cycle_id IS NOT NULL
GROUP BY mrs.cycle_id
ORDER BY readings_count DESC;

