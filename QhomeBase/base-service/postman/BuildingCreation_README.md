# Building Creation API - Postman Collection

## 📋 Mô tả
Collection này dùng để test API tạo và quản lý building với code generation tự động dựa trên tenant.

## 🚀 Cách sử dụng

### 1. Import Collection và Environment
1. Mở Postman
2. Import file `BuildingCreationAPI.postman_collection.json`
3. Import file `BuildingCreation_Environment.postman_environment.json`
4. Chọn environment "Building Creation Environment"

### 2. Đảm bảo Services đang chạy
- **iam-service**: `http://localhost:8088`
- **base-service**: `http://localhost:8081`

### 3. Chạy test theo thứ tự

#### **Test 1: Generate Test Token**
- Tạo JWT token với permission `base.building.create`
- Token sẽ được lưu vào environment variable `jwt_token`

#### **Test 2: Create Building**
- Tạo building đầu tiên
- Kiểm tra code được generate (format: `{tenantCode}{số}`)
- Building ID và code được lưu vào environment

#### **Test 3: Create Another Building**
- Tạo building thứ 2
- Kiểm tra code khác với building đầu tiên
- Test logic tăng số thứ tự

#### **Test 4: Get All Buildings**
- Lấy danh sách tất cả buildings của tenant
- Kiểm tra buildings đã tạo có trong danh sách

#### **Test 5: Test Validation - Missing Name**
- Test validation khi thiếu trường `name` (required)
- Expect status 400

#### **Test 6: Test Unauthorized - No Token**
- Test 401 khi không có token
- Expect status 401

#### **Test 7: Test Forbidden - No Permission**
- Test 403 khi token không có permission `base.building.create`
- Expect status 403

## 🔧 Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `base_service_url` | URL của base-service | `http://localhost:8081` |
| `iam_service_url` | URL của iam-service | `http://localhost:8088` |
| `jwt_token` | JWT token với permission | Auto-generated |
| `jwt_token_no_permission` | JWT token không có permission | Auto-generated |
| `created_building_id` | ID của building đã tạo | Auto-generated |
| `created_building_code` | Code của building đã tạo | Auto-generated |

## 📊 Expected Results

### **Successful Building Creation:**
```json
{
  "id": "uuid",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000",
  "code": "Tenant01",  // ← Code được tạo động
  "name": "Tòa A - Chung cư ABC",
  "address": "123 Đường ABC, Quận 1, TP.HCM",
  "floorsMax": 0,
  "totalApartmentsAll": 0,
  "totalApartmentsActive": 0
}
```

### **Code Generation Logic:**
- **Tenant có code**: `{tenantCode}{số}` → "ABC01", "ABC02", "ABC03"...
- **Tenant không có code**: `Tenant{số}` → "Tenant01", "Tenant02", "Tenant03"...

## 🐛 Troubleshooting

### **Lỗi 401 Unauthorized:**
- Kiểm tra iam-service có chạy không
- Kiểm tra token có được generate đúng không

### **Lỗi 403 Forbidden:**
- Kiểm tra token có permission `base.building.create` không
- Kiểm tra user có role `tenant_manager` hoặc `tenant_owner` không

### **Lỗi 500 Internal Server Error:**
- Kiểm tra base-service có chạy không
- Kiểm tra database connection
- Kiểm tra logs của base-service

### **Code không được generate:**
- Kiểm tra tenant có tồn tại trong database không
- Kiểm tra tenant có code không
- Kiểm tra logs của base-service

## 📝 Notes

- Collection sử dụng test scripts để tự động lưu token và building info
- Mỗi request có validation tests để đảm bảo response đúng format
- Environment variables được tự động cập nhật sau mỗi request thành công
- Có thể chạy toàn bộ collection hoặc từng request riêng lẻ



