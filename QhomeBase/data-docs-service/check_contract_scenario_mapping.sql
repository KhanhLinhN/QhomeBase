-- ========================================
-- KIỂM TRA CÁC CĂN HỘ SẼ TRIGGER SCENARIO NÀO
-- ========================================

-- Query này sẽ hiển thị mapping giữa các căn hộ và scenarios
-- Dựa trên logic phân bổ: ROW_NUMBER() % 5

WITH numbered_units AS (
    SELECT 
        u.id,
        u.code as unit_code,
        u.area_m2,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A'
      AND u.status = 'ACTIVE'
)
SELECT 
    nu.building_code,
    nu.unit_code,
    nu.rn,
    nu.rn % 5 as remainder,
    -- Xác định scenario sẽ được gán
    CASE 
        -- RENEWAL SCENARIOS
        WHEN nu.rn % 5 = 1 AND nu.rn <= 1 THEN 
            'Scenario 1: PENDING (Reminder 1) - endDate +30 days, renewal_status=PENDING'
        WHEN nu.rn % 5 = 2 THEN 
            'Scenario 2: REMINDED-2 (Reminder 2) - endDate cuối tháng, đã nhắc lần 1, trigger nếu hôm nay là ngày 8-20'
        WHEN nu.rn % 5 = 3 THEN 
            'Scenario 3: REMINDED-3 (Reminder 3) - endDate cuối tháng, đã nhắc lần 1, trigger nếu hôm nay là ngày 20'
        WHEN nu.rn % 5 = 4 THEN 
            'Scenario 4: REMINDED-21DAYS (Sẽ bị DECLINED) - đã nhắc 21 ngày trước'
        WHEN nu.rn % 5 = 0 THEN 
            'Scenario 5: DECLINED - đã bị đánh dấu DECLINED'
        
        -- STATUS SCENARIOS (chỉ áp dụng nếu rn > một số giá trị)
        WHEN nu.rn % 5 = 1 AND nu.rn > 1 AND nu.rn <= 6 THEN 
            'Scenario 6: INACTIVE-1DAY - sẽ activate ngày mai'
        WHEN nu.rn % 5 = 2 AND nu.rn > 2 THEN 
            'Scenario 7: INACTIVE-7DAYS - sẽ activate sau 7 ngày'
        WHEN nu.rn % 5 = 3 AND nu.rn > 3 THEN 
            'Scenario 8: EXPIRED - status=EXPIRED'
        WHEN nu.rn % 5 = 4 AND nu.rn > 4 THEN 
            'Scenario 9: CANCELLED - status=CANCELLED'
        WHEN nu.rn % 5 = 0 AND nu.rn > 5 THEN 
            'Scenario 10: NO-ENDDATE - không có endDate'
        WHEN nu.rn % 5 = 1 AND nu.rn > 6 THEN 
            'Scenario 11: PURCHASE - contract_type=PURCHASE'
        
        ELSE 'Không được gán scenario'
    END as assigned_scenario,
    -- Thông tin chi tiết về trigger
    CASE 
        WHEN nu.rn % 5 = 1 AND nu.rn <= 1 THEN 
            '✅ SẼ TRIGGER NGAY: endDate trong 28-32 ngày, chưa gửi nhắc'
        WHEN nu.rn % 5 = 2 THEN 
            CASE 
                WHEN EXTRACT(DAY FROM CURRENT_DATE) >= 8 AND EXTRACT(DAY FROM CURRENT_DATE) <= 20 THEN
                    '✅ SẼ TRIGGER HÔM NAY: Hôm nay là ngày ' || EXTRACT(DAY FROM CURRENT_DATE) || ' (trong khoảng 8-20)'
                ELSE
                    '⏳ CHỜ TRIGGER: Cần chạy vào ngày 8-20 của tháng endDate'
            END
        WHEN nu.rn % 5 = 3 THEN 
            CASE 
                WHEN EXTRACT(DAY FROM CURRENT_DATE) = 20 THEN
                    '✅ SẼ TRIGGER HÔM NAY: Hôm nay là ngày 20'
                ELSE
                    '⏳ CHỜ TRIGGER: Cần chạy vào ngày 20 của tháng endDate'
            END
        WHEN nu.rn % 5 = 4 THEN 
            '✅ SẼ TRIGGER NGAY: Đã nhắc 21 ngày trước, sẽ bị đánh dấu DECLINED'
        WHEN nu.rn % 5 = 0 THEN 
            'ℹ️ ĐÃ DECLINED: Không trigger reminder nữa'
        ELSE 
            'ℹ️ STATUS SCENARIO: Không liên quan đến reminder'
    END as trigger_status
FROM numbered_units nu
ORDER BY 
    nu.building_code,
    nu.rn;

-- ========================================
-- TÓM TẮT PHÂN BỔ THEO REMAINDER
-- ========================================

SELECT 
    'Tóm tắt phân bổ scenarios' as info,
    'rn % 5 = 1' as remainder_1,
    'rn % 5 = 2' as remainder_2,
    'rn % 5 = 3' as remainder_3,
    'rn % 5 = 4' as remainder_4,
    'rn % 5 = 0' as remainder_0
UNION ALL
SELECT 
    'Renewal Scenarios',
    'Scenario 1: PENDING (Reminder 1)',
    'Scenario 2: REMINDED-2 (Reminder 2)',
    'Scenario 3: REMINDED-3 (Reminder 3)',
    'Scenario 4: REMINDED-21DAYS',
    'Scenario 5: DECLINED'
UNION ALL
SELECT 
    'Status Scenarios (rn > X)',
    'Scenario 6: INACTIVE-1DAY (rn>1) + Scenario 11: PURCHASE (rn>6)',
    'Scenario 7: INACTIVE-7DAYS (rn>2)',
    'Scenario 8: EXPIRED (rn>3)',
    'Scenario 9: CANCELLED (rn>4)',
    'Scenario 10: NO-ENDDATE (rn>5)';

-- ========================================
-- VÍ DỤ CỤ THỂ: Căn hộ nào trong building B
-- ========================================

WITH numbered_units AS (
    SELECT 
        u.id,
        u.code as unit_code,
        u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code = 'B'  -- Ví dụ với building B
      AND u.status = 'ACTIVE'
)
SELECT 
    nu.unit_code,
    nu.rn,
    nu.rn % 5 as remainder,
    CASE nu.rn % 5
        WHEN 1 THEN 
            CASE 
                WHEN nu.rn = 1 THEN 'Scenario 1: PENDING (Reminder 1)'
                WHEN nu.rn = 6 THEN 'Scenario 6: INACTIVE-1DAY'
                WHEN nu.rn > 6 THEN 'Scenario 11: PURCHASE'
                ELSE 'Scenario 1: PENDING (Reminder 1)'
            END
        WHEN 2 THEN 
            CASE 
                WHEN nu.rn = 2 THEN 'Scenario 2: REMINDED-2 (Reminder 2)'
                WHEN nu.rn > 2 THEN 'Scenario 7: INACTIVE-7DAYS'
                ELSE 'Scenario 2: REMINDED-2 (Reminder 2)'
            END
        WHEN 3 THEN 
            CASE 
                WHEN nu.rn = 3 THEN 'Scenario 3: REMINDED-3 (Reminder 3)'
                WHEN nu.rn > 3 THEN 'Scenario 8: EXPIRED'
                ELSE 'Scenario 3: REMINDED-3 (Reminder 3)'
            END
        WHEN 4 THEN 
            CASE 
                WHEN nu.rn = 4 THEN 'Scenario 4: REMINDED-21DAYS'
                WHEN nu.rn > 4 THEN 'Scenario 9: CANCELLED'
                ELSE 'Scenario 4: REMINDED-21DAYS'
            END
        WHEN 0 THEN 
            CASE 
                WHEN nu.rn = 5 THEN 'Scenario 5: DECLINED'
                WHEN nu.rn > 5 THEN 'Scenario 10: NO-ENDDATE'
                ELSE 'Scenario 5: DECLINED'
            END
    END as scenario_name
FROM numbered_units nu
ORDER BY nu.rn
LIMIT 20; -- Hiển thị 20 căn đầu tiên làm ví dụ


