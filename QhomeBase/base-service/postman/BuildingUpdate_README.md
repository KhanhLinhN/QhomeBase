# Building Update API Testing Guide

## Tổng quan
Hướng dẫn test API update building với các test case khác nhau.

## Test Cases

### 1. **Generate Test Token** 
- **Mục đích**: Tạo JWT token có permission `base.building.create` và `base.building.update`
- **Expected**: Status 200, token được lưu vào environment variable `jwt_token`

### 2. **Generate Test Token No Permission**
- **Mục đích**: Tạo JWT token chỉ có permission `base.building.create` (không có `base.building.update`)
- **Expected**: Status 200, token được lưu vào environment variable `jwt_token_no_permission`

### 3. **Create Building**
- **Mục đích**: Tạo building để test update
- **Expected**: Status 200, building được tạo và ID được lưu vào `created_building_id`

### 4. **Update Building** ✅
- **Mục đích**: Update building với thông tin mới
- **Method**: `PUT /api/buildings/{buildingId}`
- **Body**: 
  ```json
  {
    "name": "Updated Building Name",
    "address": "Updated Building Address"
  }
  ```
- **Expected**: 
  - Status 200
  - Response có `id`, `name`, `address`
  - `name` = "Updated Building Name"
  - `address` = "Updated Building Address"

### 5. **Update Building - No Permission** ❌
- **Mục đích**: Test 403 Forbidden khi không có permission `base.building.update`
- **Method**: `PUT /api/buildings/{buildingId}`
- **Token**: `jwt_token_no_permission` (chỉ có `base.building.create`)
- **Expected**: Status 403

### 6. **Update Building - Not Found** ❌
- **Mục đích**: Test 404 Not Found khi building không tồn tại
- **Method**: `PUT /api/buildings/00000000-0000-0000-0000-000000000000`
- **Expected**: Status 404

## Cách chạy test

### Bước 1: Import Collection và Environment
1. Import `BuildingCreationAPI.postman_collection.json`
2. Import `BuildingCreation_Environment.postman_environment.json`
3. Chọn environment "Building Creation Environment"

### Bước 2: Chạy test theo thứ tự
1. **1. Generate Test Token** - Tạo token có đầy đủ permission
2. **1.1. Generate Test Token No Permission** - Tạo token thiếu permission
3. **2. Create Building** - Tạo building để test
4. **4. Update Building** - Test update thành công
5. **5. Update Building - No Permission** - Test 403 Forbidden
6. **6. Update Building - Not Found** - Test 404 Not Found

### Bước 3: Kiểm tra kết quả
- Tất cả test cases phải pass
- Kiểm tra console log để xem token và response
- Kiểm tra environment variables đã được set đúng

## Authorization Logic

### User có thể update building nếu:
1. **Chủ tenant**: `building.tenantId == user.tenant`
2. **User có quyền**: `user.permissions` chứa `"base.building.update"`

### Multi-tenant Support
- User có permission `base.building.update` có thể update building của bất kỳ tenant nào
- User chỉ có permission `base.building.create` chỉ có thể tạo building, không update được

## Troubleshooting

### Lỗi 403 Forbidden
- Kiểm tra token có permission `base.building.update` không
- Kiểm tra user có phải chủ tenant của building không

### Lỗi 404 Not Found
- Kiểm tra `buildingId` có đúng không
- Kiểm tra building có tồn tại trong database không

### Lỗi 401 Unauthorized
- Kiểm tra token có hợp lệ không
- Kiểm tra token có hết hạn không
- Restart iam-service nếu cần



