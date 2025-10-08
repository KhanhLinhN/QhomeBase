# Hướng dẫn Test API Tenant Deletion

## Tổng quan
Collection này dùng để test chức năng gửi và chấp nhận deletion request trong hệ thống QhomeBase.

## Cài đặt

### 1. Import vào Postman
1. Mở Postman
2. Click **Import** 
3. Chọn file `TenantDeletionAPI.postman_collection.json`
4. Chọn file `QhomeBase_Environment.postman_environment.json`
5. Click **Import**

### 2. Cấu hình Environment
1. Chọn environment **QhomeBase Environment** ở góc trên bên phải
2. Kiểm tra các biến môi trường:
   - `base_service_url`: http://localhost:8081
   - `iam_service_url`: http://localhost:8082
   - `user_id`: UUID của user test
   - `tenant_id`: UUID của tenant test
   - `username`: Tên user test

## Chạy Test

### Cách 1: Chạy từng request riêng lẻ
1. **Generate Test Token**: Tạo JWT token để authenticate
2. **Create Deletion Request**: Tạo deletion request
3. **Approve Deletion Request**: Chấp nhận deletion request

### Cách 2: Chạy toàn bộ collection
1. Click vào collection **Tenant Deletion API**
2. Click **Run** 
3. Chọn environment **QhomeBase Environment**
4. Click **Run Tenant Deletion API**

## Các Test Case

### ✅ Happy Path Tests
1. **Generate Test Token** - Tạo JWT token thành công
2. **Create Deletion Request** - Tạo deletion request với dữ liệu hợp lệ
3. **Approve Deletion Request** - Chấp nhận deletion request

### ❌ Error Tests
4. **Test Create with Invalid Tenant ID** - Test với tenant ID không thuộc về user
5. **Test Create without Token** - Test không có JWT token
6. **Test Approve with Empty Note** - Test approve với note rỗng

## Kết quả mong đợi

### Request 1: Generate Test Token
- **Status**: 200 OK
- **Response**: 
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "message": "Token generated successfully"
}
```

### Request 2: Create Deletion Request
- **Status**: 200 OK
- **Response**:
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "tenantId": "550e8400-e29b-41d4-a716-446655440001",
    "requestedBy": "550e8400-e29b-41d4-a716-446655440000",
    "approvedBy": null,
    "reason": "Test deletion request - Tenant không còn sử dụng hệ thống",
    "note": null,
    "status": "PENDING",
    "createdAt": "2024-01-15T10:30:00Z",
    "approvedAt": null
}
```

### Request 3: Approve Deletion Request
- **Status**: 200 OK
- **Response**:
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "tenantId": "550e8400-e29b-41d4-a716-446655440001",
    "requestedBy": "550e8400-e29b-41d4-a716-446655440000",
    "approvedBy": "550e8400-e29b-41d4-a716-446655440000",
    "reason": "Test deletion request - Tenant không còn sử dụng hệ thống",
    "note": "Đã phê duyệt xóa tenant",
    "status": "APPROVED",
    "createdAt": "2024-01-15T10:30:00Z",
    "approvedAt": "2024-01-15T10:35:00Z"
}
```

### Error Cases
- **Request 4**: 403 Forbidden (Invalid tenant ID)
- **Request 5**: 401 Unauthorized (No token)
- **Request 6**: 400 Bad Request (Empty note)

## Lưu ý

1. **Thứ tự chạy**: Phải chạy theo thứ tự từ 1-6 để test đầy đủ
2. **Environment**: Đảm bảo chọn đúng environment
3. **Services**: Đảm bảo base-service (port 8081) và iam-service (port 8082) đang chạy
4. **Database**: Đảm bảo PostgreSQL đang chạy và có dữ liệu test

## Troubleshooting

### Lỗi 401 Unauthorized
- Kiểm tra JWT token có được tạo thành công không
- Kiểm tra token có hết hạn không
- Kiểm tra secret key trong application.properties

### Lỗi 403 Forbidden
- Kiểm tra user có quyền `tenant_manager` hoặc `tenant_owner` không
- Kiểm tra user có permission `base.tenant.delete.request` hoặc `base.tenant.delete.approve` không
- Kiểm tra tenant_id trong token có khớp với tenant_id trong request không

### Lỗi 500 Internal Server Error
- Kiểm tra database connection
- Kiểm tra migration V7 đã chạy thành công chưa
- Kiểm tra log của base-service

## Cấu trúc Database

### Table: tenant_deletion_requests
```sql
CREATE TABLE data.tenant_deletion_requests (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    requested_by UUID NOT NULL,
    reason       TEXT,
    approved_by  UUID NULL,
    note         TEXT,
    status       data.tenant_deletion_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at  TIMESTAMPTZ NULL
);
```

### Enum: tenant_deletion_status
```sql
CREATE TYPE data.tenant_deletion_status AS ENUM ('PENDING','APPROVED','REJECTED','CANCELED');
```

