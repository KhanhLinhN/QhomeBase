-- ============================================
-- TEST QUERIES - Chạy từng query để debug
-- ============================================

-- 1. QUERY CHÍNH VỚI FILTER (giống code Java) - VỚI NULL HANDLING
-- Test với cycle_id: 1bb937b0-bdda-4ef4-a393-f13fb89cf83b
-- serviceCode: ELECTRIC, month: 2025-11
SELECT u.building_id as buildingId,
       b.code as buildingCode,
       b.name as buildingName,
       i.status as status,
       COALESCE(SUM(
           CASE 
               WHEN (:serviceCode IS NULL OR il.service_code = :serviceCode)
                   AND (:serviceMonth IS NULL OR TO_CHAR(il.service_date, 'YYYY-MM') = :serviceMonth)
               THEN (il.quantity * il.unit_price) + il.tax_amount
               ELSE 0
           END
       ), 0) as totalAmount,
       COUNT(DISTINCT CASE 
           WHEN (:serviceCode IS NULL OR il.service_code = :serviceCode)
               AND (:serviceMonth IS NULL OR TO_CHAR(il.service_date, 'YYYY-MM') = :serviceMonth)
           THEN i.id
           ELSE NULL
       END) as invoiceCount
FROM billing.invoices i
LEFT JOIN billing.invoice_lines il ON il.invoice_id = i.id
LEFT JOIN data.units u ON u.id = i.payer_unit_id
LEFT JOIN data.buildings b ON b.id = u.building_id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
  AND u.building_id IS NOT NULL
GROUP BY u.building_id, b.code, b.name, i.status
HAVING (
    (:serviceCode IS NULL AND :serviceMonth IS NULL)
    OR SUM(CASE 
        WHEN (:serviceCode IS NULL OR il.service_code = :serviceCode)
            AND (:serviceMonth IS NULL OR TO_CHAR(il.service_date, 'YYYY-MM') = :serviceMonth)
        THEN 1
        ELSE 0
    END) > 0
);

-- 1a. QUERY CHÍNH VỚI FILTER (không dùng NULL, dùng giá trị cụ thể)
-- Test với ELECTRIC và 2025-11
SELECT u.building_id as buildingId,
       b.code as buildingCode,
       b.name as buildingName,
       i.status as status,
       COALESCE(SUM(
           CASE 
               WHEN il.service_code = 'ELECTRIC'
                   AND TO_CHAR(il.service_date, 'YYYY-MM') = '2025-11'
               THEN (il.quantity * il.unit_price) + il.tax_amount
               ELSE 0
           END
       ), 0) as totalAmount,
       COUNT(DISTINCT CASE 
           WHEN il.service_code = 'ELECTRIC'
               AND TO_CHAR(il.service_date, 'YYYY-MM') = '2025-11'
           THEN i.id
           ELSE NULL
       END) as invoiceCount
FROM billing.invoices i
LEFT JOIN billing.invoice_lines il ON il.invoice_id = i.id
LEFT JOIN data.units u ON u.id = i.payer_unit_id
LEFT JOIN data.buildings b ON b.id = u.building_id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
  AND u.building_id IS NOT NULL
GROUP BY u.building_id, b.code, b.name, i.status
HAVING SUM(CASE 
    WHEN il.service_code = 'ELECTRIC'
        AND TO_CHAR(il.service_date, 'YYYY-MM') = '2025-11'
    THEN 1
    ELSE 0
END) > 0;

-- 2. QUERY KIỂM TRA DỮ LIỆU CƠ BẢN (không filter)
-- Xem có invoices nào trong cycle này không
SELECT 
    i.id as invoice_id,
    i.status,
    u.building_id,
    b.code as building_code,
    b.name as building_name,
    COUNT(il.id) as line_count
FROM billing.invoices i
LEFT JOIN billing.invoice_lines il ON il.invoice_id = i.id
LEFT JOIN data.units u ON u.id = i.payer_unit_id
LEFT JOIN data.buildings b ON b.id = u.building_id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
GROUP BY i.id, i.status, u.building_id, b.code, b.name
ORDER BY u.building_id, i.status;

-- 3. QUERY XEM CHI TIẾT INVOICE_LINES
-- Xem service_code và service_date của từng line
SELECT 
    i.id as invoice_id,
    i.status as invoice_status,
    u.building_id,
    b.code as building_code,
    b.name as building_name,
    il.service_code,
    il.service_date,
    TO_CHAR(il.service_date, 'YYYY-MM') as service_month,
    (il.quantity * il.unit_price) + il.tax_amount as line_total
FROM billing.invoices i
LEFT JOIN billing.invoice_lines il ON il.invoice_id = i.id
LEFT JOIN data.units u ON u.id = i.payer_unit_id
LEFT JOIN data.buildings b ON b.id = u.building_id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
  AND u.building_id IS NOT NULL
ORDER BY u.building_id, il.service_code, il.service_date;

-- 4. QUERY KIỂM TRA FILTER
-- Đếm số invoice_lines có service_code = 'ELECTRIC' và tháng 2025-11
SELECT 
    COUNT(*) as total_lines,
    COUNT(DISTINCT i.id) as invoice_count,
    COUNT(DISTINCT u.building_id) as building_count
FROM billing.invoices i
INNER JOIN billing.invoice_lines il ON il.invoice_id = i.id
INNER JOIN data.units u ON u.id = i.payer_unit_id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
  AND il.service_code = 'ELECTRIC'
  AND TO_CHAR(il.service_date, 'YYYY-MM') = '2025-11'
  AND u.building_id IS NOT NULL;

-- 5. QUERY ĐƠN GIẢN KHÔNG FILTER
-- Để so sánh kết quả - xem có buildings nào không
SELECT u.building_id as buildingId,
       b.code as buildingCode,
       b.name as buildingName,
       i.status as status,
       COALESCE(SUM((il.quantity * il.unit_price) + il.tax_amount), 0) as totalAmount,
       COUNT(DISTINCT i.id) as invoiceCount
FROM billing.invoices i
LEFT JOIN billing.invoice_lines il ON il.invoice_id = i.id
LEFT JOIN data.units u ON u.id = i.payer_unit_id
LEFT JOIN data.buildings b ON b.id = u.building_id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
  AND u.building_id IS NOT NULL
GROUP BY u.building_id, b.code, b.name, i.status;

-- 6. QUERY XEM TẤT CẢ SERVICE_CODE TRONG CYCLE
-- Xem cycle này có những service_code nào
SELECT DISTINCT
    il.service_code,
    TO_CHAR(il.service_date, 'YYYY-MM') as service_month,
    COUNT(*) as line_count
FROM billing.invoices i
INNER JOIN billing.invoice_lines il ON il.invoice_id = i.id
WHERE i.cycle_id = '1bb937b0-bdda-4ef4-a393-f13fb89cf83b'::uuid
GROUP BY il.service_code, TO_CHAR(il.service_date, 'YYYY-MM')
ORDER BY il.service_code, service_month;

