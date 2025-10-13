# Unit Management API Testing Guide

## Tổng quan
Postman collection này cung cấp đầy đủ các test case cho Unit Management API, bao gồm CRUD operations, query operations và error handling.

## Cài đặt

### 1. Import Collection
- Mở Postman
- Click "Import" 
- Chọn file `UnitManagementAPI.postman_collection.json`

### 2. Import Environment
- Click "Import" 
- Chọn file `QhomeBase_Environment.postman_environment.json`
- Chọn environment "QhomeBase Environment"

### 3. Cấu hình Environment Variables
Đảm bảo các biến sau được cấu hình đúng:
- `base_url`: http://localhost:8081
- `tenant_id`: 550e8400-e29b-41d4-a716-446655440001
- `building_id`: 550e8400-e29b-41d4-a716-446655440003
- `jwt_token`: (sẽ được tự động set khi chạy "Generate Test Token")

## Cách sử dụng

### Bước 1: Authentication
1. **Chạy request "Generate Test Token"** trong folder "Authentication"
   - Endpoint: `POST {{iam_service_url}}/api/test/generate-token`
   - Cần body với user information và permissions
   - Token sẽ được tự động lưu vào environment variable

2. **(Tùy chọn) Chạy "Check Token"** để verify token
   - Endpoint: `POST {{iam_service_url}}/api/test/generate-token` với Authorization header
   - Kiểm tra token có hoạt động không

### Bước 2: Test CRUD Operations
Chạy các request theo thứ tự:

1. **Create Unit - Basic**: Tạo unit trên tầng 1
2. **Create Unit - Floor 2**: Tạo unit trên tầng 2  
3. **Get Unit by ID**: Lấy thông tin unit vừa tạo
4. **Update Unit**: Cập nhật thông tin unit
5. **Change Unit Status**: Thay đổi status unit
6. **Delete Unit**: Soft delete unit

### Bước 3: Test Query Operations
1. **Get Units by Building ID**: Lấy tất cả units trong building
2. **Get Units by Tenant ID**: Lấy tất cả units của tenant
3. **Get Units by Floor**: Lấy units theo tầng

### Bước 4: Test Error Cases
1. **Create Unit - Invalid Building ID**: Test với building ID không tồn tại
2. **Get Unit - Invalid ID**: Test với unit ID không tồn tại
3. **Update Unit - Invalid ID**: Test update unit không tồn tại
4. **Create Unit - Missing Required Fields**: Test validation

## API Endpoints

### Test Authentication
- `POST {{iam_service_url}}/api/test/generate-token` - Generate test JWT token (không cần auth)
- `POST {{iam_service_url}}/api/test/generate-token` - Check token validity (cần Authorization header)

### CRUD Operations
- `POST /api/units` - Tạo unit mới
- `GET /api/units/{id}` - Lấy unit theo ID
- `PUT /api/units/{id}` - Cập nhật unit
- `DELETE /api/units/{id}` - Xóa unit (soft delete)
- `PATCH /api/units/{id}/status` - Thay đổi status unit

### Query Operations
- `GET /api/units/building/{buildingId}` - Lấy units theo building
- `GET /api/units/tenant/{tenantId}` - Lấy units theo tenant
- `GET /api/units/building/{buildingId}/floor/{floor}` - Lấy units theo tầng

## Test Token Details

### User Information
- **User ID**: 550e8400-e29b-41d4-a716-446655440000
- **Username**: testuser
- **Tenant ID**: 550e8400-e29b-41d4-a716-446655440001
- **Roles**: tenant_manager, tenant_owner
- **Permissions**: 
  - base.unit.create
  - base.unit.update
  - base.unit.view
  - base.unit.delete
  - base.unit.status.manage
  - base.building.create
  - base.building.update
  - base.building.delete.request
  - base.building.delete.approve

## Test Cases

### ✅ Positive Test Cases
- Tạo unit thành công
- Lấy thông tin unit
- Cập nhật unit
- Thay đổi status unit
- Xóa unit (soft delete)
- Query units theo building/tenant/floor

### ❌ Negative Test Cases
- Tạo unit với building ID không tồn tại
- Lấy unit với ID không tồn tại
- Cập nhật unit với ID không tồn tại
- Tạo unit thiếu required fields

## Expected Results

### Status Codes
- `200 OK`: Request thành công
- `400 Bad Request`: Validation error hoặc bad request
- `404 Not Found`: Resource không tồn tại
- `401 Unauthorized`: Token không hợp lệ hoặc thiếu

### Response Format
```json
{
    "id": "uuid",
    "tenantId": "uuid", 
    "buildingId": "uuid",
    "buildingCode": "string",
    "buildingName": "string",
    "code": "string",
    "floor": 1,
    "areaM2": 50.5,
    "bedrooms": 2,
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
}
```

## Troubleshooting

### Lỗi thường gặp

1. **401 Unauthorized**
   - Kiểm tra JWT token có hợp lệ không
   - Chạy lại "Generate Test Token"

2. **404 Not Found**
   - Kiểm tra building_id và tenant_id có đúng không
   - Đảm bảo test data đã được seed

3. **400 Bad Request**
   - Kiểm tra request body format
   - Kiểm tra required fields

### Debug Steps
1. Kiểm tra console logs trong Postman
2. Kiểm tra environment variables
3. Kiểm tra server logs
4. Verify database có test data

## Notes
- Collection có pre-request script để check JWT token
- Các test script tự động set environment variables
- Có thể chạy toàn bộ collection hoặc từng request riêng lẻ
- Sử dụng IAM service endpoint để generate token: `{{iam_service_url}}/api/test/generate-token`
- Đảm bảo IAM service đang chạy trên port 8088
