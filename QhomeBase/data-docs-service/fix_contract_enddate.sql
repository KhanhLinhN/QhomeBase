-- ========================================
-- SỬA ENDDATE CHO CONTRACT CỤ THỂ
-- ========================================

-- Sửa endDate cho contract TEST-RENEWAL-E-E---03-REMINDED-20DAYS
-- Start date: 17/12/2024
-- End date hiện tại: 20/12/2025 (sai)
-- End date mong muốn: 17/11/2025 (startDate + 11 tháng)

UPDATE files.contracts
SET 
    end_date = start_date + INTERVAL '11 months',
    updated_at = NOW()
WHERE contract_number = 'TEST-RENEWAL-E-E---03-REMINDED-20DAYS'
  AND start_date = '2024-12-17'::date;

-- Kiểm tra kết quả
SELECT 
    contract_number,
    start_date,
    end_date,
    end_date - start_date as contract_duration,
    end_date - CURRENT_DATE as days_until_expiry,
    renewal_status,
    renewal_reminder_sent_at
FROM files.contracts
WHERE contract_number = 'TEST-RENEWAL-E-E---03-REMINDED-20DAYS';

-- ========================================
-- SỬA TẤT CẢ CONTRACTS REMINDED-3 CÓ ENDDATE SAI
-- ========================================

-- Sửa tất cả contracts REMINDED-3 có endDate không hợp lý
-- (endDate quá xa so với startDate)
UPDATE files.contracts
SET 
    end_date = start_date + INTERVAL '11 months',
    updated_at = NOW()
WHERE contract_number LIKE 'TEST-RENEWAL-%-REMINDED-3'
  AND end_date > start_date + INTERVAL '1 year'
  AND start_date IS NOT NULL;

-- Kiểm tra tất cả contracts REMINDED-3
SELECT 
    contract_number,
    start_date,
    end_date,
    end_date - start_date as contract_duration,
    end_date - CURRENT_DATE as days_until_expiry,
    CASE 
        WHEN end_date > start_date + INTERVAL '1 year' THEN 'ENDDATE_QUA_XA'
        WHEN end_date < start_date THEN 'ENDDATE_TRUOC_STARTDATE'
        WHEN end_date - CURRENT_DATE > 30 THEN 'CON_HON_30_NGAY'
        WHEN end_date - CURRENT_DATE BETWEEN 1 AND 30 THEN 'TRONG_KHOANG_1_30_NGAY'
        WHEN end_date - CURRENT_DATE <= 0 THEN 'DA_HET_HAN'
        ELSE 'OK'
    END as enddate_status
FROM files.contracts
WHERE contract_number LIKE 'TEST-RENEWAL-%-REMINDED-3'
ORDER BY contract_number;

