# Đánh Giá Cấu Trúc Luồng Meter Reading

## 📊 Tổng Quan Cấu Trúc

### Entities và Relationships

1. **ReadingCycle** (Chu kỳ đọc)
   - `period_from`, `period_to`: Thời gian chu kỳ
   - `status`: OPEN/CLOSED
   - ✓ Ổn

2. **MeterReadingAssignment** (Phân công đọc)
   - `cycle_id`: FK → ReadingCycle
   - `building_id`: FK → Building (nullable)
   - `service_id`: FK → Service
   - `floor`: INTEGER (nullable) - Tầng cụ thể
   - `unit_ids`: UUID[] (nullable) - Danh sách unit cụ thể (ngoại lệ)
   - `status`: PENDING/IN_PROGRESS/COMPLETED/CANCELLED/OVERDUE
   - `start_date`, `end_date`: Thời gian thực hiện
   - ✓ Cấu trúc tốt, hỗ trợ cả floor và unit_ids

3. **MeterReading** (Dữ liệu đọc)
   - `meter_id`: FK → Meter
   - `unit_id`: FK → Unit (denormalized cho performance)
   - `assignment_id`: FK → MeterReadingAssignment (nullable)
   - `reading_date`: Ngày đọc
   - `prev_index`, `curr_index`: Chỉ số
   - `verified`: Boolean
   - ✓ Cấu trúc tốt

4. **Meter** (Đồng hồ)
   - `unit_id`: FK → Unit
   - `service_id`: FK → Service
   - `active`: Boolean
   - ✓ Cấu trúc tốt

## 🔄 Luồng Hoạt Động

### 1. Tạo Reading Cycle
```
Admin → Tạo ReadingCycle (period_from, period_to)
```
✓ Ổn

### 2. Tạo Assignment
```
Admin → Tạo MeterReadingAssignment
  - Chọn cycle
  - Chọn building, service
  - Chọn scope: floor hoặc unit_ids (hoặc cả 2)
  - Gán cho staff (assigned_to)
```
⚠️ **Vấn đề**: Service chưa xử lý `unit_ids` từ request

### 3. Đọc Meter
```
Staff → Tạo MeterReading
  - Chọn meter
  - Nhập chỉ số
  - Gán vào assignment (nếu có)
```
⚠️ **Vấn đề**: `validateMeterInScope` chưa kiểm tra `unit_ids`

### 4. Xác thực Reading
```
Admin/Accountant → Verify reading
  - Set verified = true
  - verified_by, verified_at
```
✓ Ổn

## ⚠️ Các Vấn Đề Cần Xử Lý

### 1. Service Logic Chưa Xử Lý `unit_ids`

**Vấn đề**: 
- `MeterReadingAssignmentService.create()` không set `unit_ids` từ request
- `MeterReadingAssignmentService.toDto()` không map `unit_ids`
- `MeterReadingAssignmentService.validateNoOverlap()` chỉ check floor, chưa check `unit_ids`

**Cần sửa**:
```java
// create() method
.unitIds(req.unitIds())  // Thêm dòng này

// toDto() method
assignment.getUnitIds(),  // Thêm field này

// validateNoOverlap() method
// Cần thêm logic check unit_ids overlap
```

### 2. Validation Chưa Đầy Đủ

**Vấn đề**: 
- `MeterReadingService.validateMeterInScope()` chỉ check floor, chưa check `unit_ids`

**Cần sửa**:
```java
// validateMeterInScope() method
if (a.getUnitIds() != null && !a.getUnitIds().contains(m.getUnit().getId())) {
    throw new IllegalArgumentException("Unit not in assignment scope");
}
```

### 3. View `v_meters_with_reading_status` Có Thể Tối Ưu

**Vấn đề**: 
- View join tất cả assignments, có thể chậm
- Cần filter theo assignment_id khi query

**Giải pháp**: 
- View đã đúng, chỉ cần filter khi query
- Có function `get_meters_with_status_for_assignment()` hỗ trợ

## ✅ Điểm Mạnh

1. **Database Schema**: 
   - Cấu trúc rõ ràng, normalization tốt
   - Có trigger validation cho `unit_ids`
   - Có indexes phù hợp

2. **Views và Functions**:
   - `v_reading_assignments_status`: Track assignment status
   - `v_reading_cycles_progress`: Track cycle progress
   - `v_meters_with_reading_status`: Track meter reading status
   - `get_meters_for_assignment()`: Get meters for assignment
   - `get_meters_with_status_for_assignment()`: Get meters with status

3. **Flexibility**:
   - Hỗ trợ cả floor-based và unit-based assignment
   - Hỗ trợ cả assignment-scoped và standalone readings

## 📝 Tóm Tắt

### ✅ Đã Ổn
- Database schema và relationships
- Views và functions
- Status management
- Trigger validation

### ⚠️ Cần Xử Lý
- Service logic xử lý `unit_ids` array
- Validation logic cho `unit_ids`
- DTO mapping cho `unit_ids`

### 🎯 Kết Luận

**Cấu trúc luồng đã ổn về mặt database và design**, nhưng **service layer chưa xử lý đầy đủ `unit_ids` array**. Cần hoàn thiện:

1. Update `create()` method để set `unit_ids`
2. Update `toDto()` method để map `unit_ids`
3. Update `validateNoOverlap()` để check `unit_ids` overlap
4. Update `validateMeterInScope()` để check `unit_ids`

Sau khi hoàn thiện các điểm trên, **cấu trúc luồng sẽ hoàn toàn ổn**.


