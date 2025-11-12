# Cách Đơn Giản: Trạng Thái Meter Reading

## Logic Đơn Giản

### 1. Khi Start Cycle Mới
- Tạo assignment → Meters trong scope = **PENDING** (mặc định)
- Không cần set trạng thái gì, chỉ cần check khi query

### 2. Khi Đọc Xong
- Tạo `MeterReading` với `assignment_id`
- Tự động status = **READ** (vì có reading record)

### 3. Query Trạng Thái
Đơn giản: LEFT JOIN với `meter_readings`
```sql
SELECT 
    m.*,
    CASE 
        WHEN mr.id IS NOT NULL THEN 'READ'
        ELSE 'PENDING'
    END AS reading_status
FROM meters m
LEFT JOIN meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = :assignment_id
)
WHERE m.active = TRUE
```

### 4. Không Cần View Phức Tạp
- Query trực tiếp trong service
- Dùng LEFT JOIN đơn giản
- Check có reading = READ, không có = PENDING

## Kết Luận

✅ **Đơn giản**: Query trực tiếp với LEFT JOIN
✅ **Không cần view phức tạp**: Chỉ cần query đơn giản
✅ **Logic rõ ràng**: Có reading = READ, không có = PENDING


