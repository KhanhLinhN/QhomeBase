# Hướng dẫn seed households cho buildings C, D, E, G

## Vấn đề
Script `seed_households_fixed.sql` yêu cầu các units phải tồn tại trước. Nếu chạy script mà chưa có units, sẽ gặp lỗi:
```
ERROR: Missing units: C1---01, C1---02, ...
```

**Lưu ý về format unit code:** Unit code phải theo format `{building_code}{floor}---{unit_number}`, ví dụ:
- `D1---01` (building D, floor 1, unit 01)
- `D2---03` (building D, floor 2, unit 03)
- `D3---05` (building D, floor 3, unit 05)

## Giải pháp

### Bước 1: Tạo buildings và units
Chạy script `create_units_for_c_d_e_g.sql` trước:
```sql
\i create_units_for_c_d_e_g.sql
```

Hoặc nếu dùng psql:
```bash
psql -U your_user -d your_database -f create_units_for_c_d_e_g.sql
```

Script này sẽ:
- Tạo buildings C, D, E, G (nếu chưa có)
- Tạo các units với format: {building_code}{floor}---{unit_number}
  - Building C: C1---01, C1---02, C2---03, C2---04, C3---05
  - Building D: D1---01, D1---02, D2---03, D2---04, D3---05
  - Building E: E1---01, E1---02, E2---03, E2---04, E3---05
  - Building G: G1---01, G1---02, G2---03, G2---04, G3---05

### Bước 2: Seed households
Sau khi units đã được tạo, chạy script seed households:
```sql
\i seed_households_fixed.sql
```

Hoặc:
```bash
psql -U your_user -d your_database -f seed_households_fixed.sql
```

## Kiểm tra kết quả

Sau khi chạy xong, kiểm tra bằng query:
```sql
SELECT 
    b.code as building_code,
    u.code as unit_code,
    r.full_name as primary_resident_name,
    r.phone,
    h.kind as household_kind,
    h.start_date,
    (SELECT COUNT(*) FROM data.household_members hm WHERE hm.household_id = h.id) as member_count
FROM data.households h
INNER JOIN data.units u ON h.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN data.residents r ON h.primary_resident_id = r.id
WHERE b.code IN ('C', 'D', 'E', 'G')
ORDER BY b.code, u.code;
```

## Lưu ý

1. Script `seed_households_fixed.sql` sẽ **TRUNCATE** bảng `data.households` và `data.household_members` - tất cả dữ liệu households hiện có sẽ bị xóa!
2. Script sẽ tạo users trong `iam.users` và `iam.user_roles` cho các residents có account
3. Một số primary residents (như C---05, D---01, etc.) không có user account - chỉ có thông tin resident
4. Tất cả spouses đều có user account

## Các vấn đề đã được sửa

1. ✅ Thêm `CREATE EXTENSION IF NOT EXISTS pgcrypto;` ở đầu script
2. ✅ Kiểm tra units tồn tại trước khi seed
3. ✅ Sử dụng `ON CONFLICT (household_id, resident_id)` cho household_members thay vì chỉ dùng id
4. ✅ Chia script thành các DO blocks nhỏ để dễ debug




