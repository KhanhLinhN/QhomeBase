# Hướng Dẫn Test Case Xóa Tenant - QhomeBase

## Tổng Quan Workflow Xóa Tenant

Quá trình xóa tenant trong QhomeBase bao gồm các bước sau:
1. **Setup**: Tạo tenant, buildings, units
2. **Request Deletion**: Tenant owner tạo request xóa
3. **Admin Review**: Admin xem và approve request
4. **Building Deletion**: Buildings chuyển sang DELETING, units sang INACTIVE
5. **Complete**: Admin complete deletion sau khi tất cả targets đã sẵn sàng

## Environment Variables Cần Thiết

```javascript
// Base URLs
base_service_url: "http://localhost:8081"
iam_service_url: "http://localhost:8080"

// Test Data IDs (sẽ được tự động generate)
tenant_id: "550e8400-e29b-41d4-a716-446655440012"
user_id: "550e8400-e29b-41d4-a716-446655440011"
building_id: "550e8400-e29b-41d4-a716-446655440014"
unit_id: "550e8400-e29b-41d4-a716-446655440015"
deletion_request_id: "550e8400-e29b-41d4-a716-446655440016"
building_deletion_request_id: "550e8400-e29b-41d4-a716-446655440017"

// Tokens (sẽ được generate từ IAM service)
access_token: ""
admin_access_token: ""
```

## 🔄 **Auto ID Management**

Postman collection đã được cập nhật để **tự động extract và lưu các IDs** từ API responses:

### **Automatic ID Extraction:**
- ✅ **tenant_id**: Tự động extract từ "Create New Tenant" response
- ✅ **building_id**: Tự động extract từ "Create Building" response  
- ✅ **unit_id**: Tự động extract từ "Create Unit" response
- ✅ **deletion_request_id**: Tự động extract từ "Create Tenant Deletion Request" response

### **Enhanced Logging:**
- ✅ **Detailed Response Logging**: In ra status, body, và parsed response
- ✅ **ID Tracking**: Hiển thị IDs đang được sử dụng
- ✅ **Error Handling**: Log lỗi chi tiết khi request fail
- ✅ **Success Confirmation**: Xác nhận khi ID được extract thành công

### **No More Manual ID Setting:**
- ❌ **Removed**: Force setting IDs trong pre-request script
- ✅ **Dynamic**: IDs được tự động extract từ responses
- ✅ **Reliable**: Đảm bảo IDs luôn đúng và up-to-date

---

## Test Case 1: Setup Environment & Authentication

### 1.1 Generate User Token (Tenant Manager)
**Method**: `POST`  
**URL**: `{{iam_service_url}}/api/test/generate-token`

**Headers**:
```json
{
  "Content-Type": "application/json"
}
```

**Body**:
```json
{
  "username": "qhomebase_manager_2025",
  "uid": "550e8400-e29b-41d4-a716-446655440011",
  "tenantId": "550e8400-e29b-41d4-a716-446655440012",
  "roles": ["tenant_manager", "tenant_owner"],
  "permissions": [
    "base.tenant.create",
    "base.tenant.read", 
    "base.tenant.update",
    "base.tenant.delete",
    "base.tenant.delete.request",
    "base.tenant.delete.approve"
  ]
}
```

**Expected Response**: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Test Script**:
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (response.token) {
        pm.environment.set('access_token', response.token);
        console.log('User token generated and stored');
        
        // Extract tenant ID from token
        try {
            const parts = response.token.split('.');
            const payload = JSON.parse(atob(parts[1]));
            const tid = payload.tenant || payload.tenantId;
            if (tid) {
                pm.environment.set('tenant_id', tid);
                console.log('Tenant ID extracted:', tid);
            }
        } catch (e) {
            console.log('Cannot decode JWT payload:', e.message);
        }
    }
}
```

### 1.2 Generate Admin Token
**Method**: `POST`  
**URL**: `{{iam_service_url}}/api/test/generate-token`

**Headers**:
```json
{
  "Content-Type": "application/json"
}
```

**Body**:
```json
{
  "username": "qhomebase_admin_2025",
  "uid": "550e8400-e29b-41d4-a716-446655440013",
  "tenantId": "550e8400-e29b-41d4-a716-446655440012",
  "roles": ["admin", "tenant_owner", "tenant_manager"],
  "permissions": [
    "base.tenant.create",
    "base.tenant.read",
    "base.tenant.update", 
    "base.tenant.delete",
    "base.tenant.delete.request",
    "base.tenant.delete.approve",
    "base.building.delete.approve"
  ]
}
```

**Expected Response**: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Test Script**:
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (response.token) {
        pm.environment.set('admin_access_token', response.token);
        console.log('Admin token generated and stored');
    }
}
```

### 1.3 Debug All Tenants
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/tenants/debug-all`

**Headers**: None

**Expected Response**: `200 OK`
```
=== ALL TENANTS IN DATABASE ===
Total tenants: 1
- ID: 550e8400-e29b-41d4-a716-446655440012, Code: QHOME2025, Name: QhomeBase Smart Tower, Status: ACTIVE, Deleted: false
================================
```

**Note**: Debug endpoint để xem tất cả tenants trong database (bao gồm cả deleted)

---

## Test Case 2: Create Test Data

### 2.1 Create New Tenant
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenants`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {{access_token}}"
}
```

**Body**:
```json
{
  "code": "QHOME2025",
  "name": "QhomeBase Smart Tower",
  "contact": "+84 901 234 567",
  "email": "contact@qhomebase2025.com",
  "address": "123 Innovation District, Ho Chi Minh City",
  "status": "ACTIVE",
  "description": "Modern smart building management system for 2025 testing"
}
```

**Expected Response**: `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440012",
  "code": "QHOME2025",
  "name": "QhomeBase Smart Tower",
  "status": "ACTIVE",
  "createdAt": "2025-01-27T10:00:00Z"
}
```

**Test Script**:
```javascript
if (pm.response.code === 201) {
    const response = pm.response.json();
    if (response.id) {
        pm.environment.set('tenant_id', response.id);
        console.log('New Tenant ID extracted:', response.id);
    }
}
```

### 2.2 Create Building
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/buildings?tenantId={{tenant_id}}`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {{access_token}}"
}
```

**Body**:
```json
{
  "name": "QhomeBase Main Building",
  "address": "456 Smart Street, Ho Chi Minh City"
}
```

**Expected Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440014",
  "name": "QhomeBase Main Building",
  "status": "ACTIVE",
  "tenantId": "550e8400-e29b-41d4-a716-446655440012"
}
```

**Test Script**:
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (response.id) {
        pm.environment.set('building_id', response.id);
        console.log('Building ID extracted:', response.id);
    }
}
```

### 2.3 Create Unit
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/units`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {{access_token}}"
}
```

**Body**:
```json
{
  "buildingId": "{{building_id}}",
  "code": "UNIT2025",
  "name": "Smart Office Unit 2025",
  "area": 85.5,
  "status": "ACTIVE",
  "description": "Modern smart office unit in QhomeBase Main Building 2025"
}
```

**Expected Response**: `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440015",
  "code": "UNIT2025",
  "name": "Smart Office Unit 2025",
  "status": "ACTIVE",
  "buildingId": "550e8400-e29b-41d4-a716-446655440014"
}
```

**Test Script**:
```javascript
if (pm.response.code === 201) {
    const response = pm.response.json();
    if (response.id) {
        pm.environment.set('unit_id', response.id);
        console.log('Unit ID extracted:', response.id);
    }
}
```

---

## Test Case 3: Tenant Deletion Workflow

### 3.1 Create Tenant Deletion Request
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenant-deletions`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {{access_token}}"
}
```

**Body**:
```json
{
  "tenantId": "{{tenant_id}}",
  "reason": "QhomeBase 2025 migration completed, legacy system no longer needed"
}
```

**Expected Response**: `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440016",
  "tenantId": "550e8400-e29b-41d4-a716-446655440012",
  "status": "PENDING",
  "reason": "QhomeBase 2025 migration completed, legacy system no longer needed",
  "createdAt": "2025-01-27T10:00:00Z"
}
```

**Test Script**:
```javascript
if (pm.response.code === 201) {
    const response = pm.response.json();
    if (response.id) {
        pm.environment.set('deletion_request_id', response.id);
        console.log('Deletion request ID extracted:', response.id);
    }
}
```

### 3.2 Get My Tenant Deletion Requests
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/tenant-deletions/my-requests`

**Headers**:
```json
{
  "Authorization": "Bearer {{access_token}}"
}
```

**Expected Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440016",
    "tenantId": "550e8400-e29b-41d4-a716-446655440012",
    "status": "PENDING",
    "reason": "QhomeBase 2025 migration completed, legacy system no longer needed",
    "createdAt": "2025-01-27T10:00:00Z"
  }
]
```

### 3.3 Get All Tenant Deletion Requests (Admin)
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/tenant-deletions`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440016",
    "tenantId": "550e8400-e29b-41d4-a716-446655440012",
    "status": "PENDING",
    "reason": "QhomeBase 2025 migration completed, legacy system no longer needed",
    "createdAt": "2025-01-27T10:00:00Z"
  }
]
```

### 3.4 Get Deletion Request Details (Admin)
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/tenant-deletions/{{deletion_request_id}}`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440016",
  "tenantId": "550e8400-e29b-41d4-a716-446655440012",
  "status": "PENDING",
  "reason": "QhomeBase 2025 migration completed, legacy system no longer needed",
  "createdAt": "2025-01-27T10:00:00Z",
  "updatedAt": "2025-01-27T10:00:00Z"
}
```

### 3.5 Get Targets Status (Admin)
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/tenant-deletions/{{deletion_request_id}}/targets-status`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440012",
  "buildings": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440014",
      "name": "QhomeBase Main Building",
      "status": "ACTIVE",
      "unitsCount": 1,
      "units": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440015",
          "code": "UNIT2025",
          "status": "ACTIVE"
        }
      ]
    }
  ]
}
```

### 3.6 Approve Tenant Deletion Request (Admin)
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenant-deletions/{{deletion_request_id}}/approve`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Body**:
```json
{
  "note": "Approved after reviewing QhomeBase 2025 migration requirements and ensuring no active dependencies"
}
```

**Expected Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440016",
  "status": "APPROVED",
  "note": "Approved after reviewing QhomeBase 2025 migration requirements and ensuring no active dependencies",
  "approvedAt": "2025-01-27T10:05:00Z"
}
```

**Expected Side Effects**:
- Buildings chuyển sang status `DELETING`
- Units chuyển sang status `INACTIVE`

---

## Test Case 4: Building Deletion Workflow

### 4.1 Get Deleting Buildings (Admin)
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/buildings/deleting`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440014",
    "name": "QhomeBase Main Building",
    "status": "DELETING",
    "tenantId": "550e8400-e29b-41d4-a716-446655440012"
  }
]
```

**Test Script**:
```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (Array.isArray(response) && response.length > 0) {
        pm.environment.set('building_deletion_request_id', response[0].id);
        console.log('Building deletion request ID extracted:', response[0].id);
    }
}
```

### 4.2 Get My Deleting Buildings
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/buildings/my-deleting-buildings?tenantId={{tenant_id}}`

**Headers**:
```json
{
  "Authorization": "Bearer {{access_token}}"
}
```

**Expected Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440014",
    "name": "QhomeBase Main Building",
    "status": "DELETING",
    "tenantId": "550e8400-e29b-41d4-a716-446655440012"
  }
]
```

### 4.3 Get Building Targets Status (Admin)
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/buildings/{{building_id}}/targets-status`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
{
  "buildingId": "550e8400-e29b-41d4-a716-446655440014",
  "units": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440015",
      "code": "UNIT2025",
      "status": "INACTIVE"
    }
  ]
}
```

### 4.4 Complete Building Deletion (Admin)
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/buildings/{{building_deletion_request_id}}/complete`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440014",
  "status": "ARCHIVED",
  "completedAt": "2025-01-27T10:10:00Z"
}
```

---

## Test Case 5: Complete Tenant Deletion

### 5.1 Complete Tenant Deletion (Admin)
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenant-deletions/{{deletion_request_id}}/complete`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440016",
  "status": "COMPLETED",
  "completedAt": "2025-01-27T10:15:00Z"
}
```

**Expected Side Effects**:
- Tenant chuyển sang status `ARCHIVED`
- Tất cả buildings và units đã được xóa hoặc archived

---

## Test Case 6: Verification & Cleanup

### 6.1 Verify Tenant Status
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/tenants/{{tenant_id}}`

**Headers**:
```json
{
  "Authorization": "Bearer {{access_token}}"
}
```

**Expected Response**: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440012",
  "code": "QHOME2025",
  "name": "QhomeBase Smart Tower",
  "status": "ARCHIVED"
}
```

### 6.2 Verify Buildings Status
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/buildings?tenantId={{tenant_id}}`

**Headers**:
```json
{
  "Authorization": "Bearer {{access_token}}"
}
```

**Expected Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440014",
    "name": "QhomeBase Main Building",
    "status": "ARCHIVED",
    "tenantId": "550e8400-e29b-41d4-a716-446655440012"
  }
]
```

### 6.3 Verify Units Status
**Method**: `GET`  
**URL**: `{{base_service_url}}/api/units?buildingId={{building_id}}`

**Headers**:
```json
{
  "Authorization": "Bearer {{access_token}}"
}
```

**Expected Response**: `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440015",
    "code": "UNIT2025",
    "name": "Smart Office Unit 2025",
    "status": "INACTIVE",
    "buildingId": "550e8400-e29b-41d4-a716-446655440014"
  }
]
```

---

## Test Case 7: Error Scenarios

### 7.1 Create Deletion Request Without Permission
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenant-deletions`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer invalid_token"
}
```

**Body**:
```json
{
  "tenantId": "{{tenant_id}}",
  "reason": "Test unauthorized access"
}
```

**Expected Response**: `401 Unauthorized`
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### 7.2 Approve Non-existent Deletion Request
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenant-deletions/invalid-id/approve`

**Headers**:
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Body**:
```json
{
  "note": "Test non-existent request"
}
```

**Expected Response**: `404 Not Found`
```json
{
  "error": "Not Found",
  "message": "Tenant deletion request not found"
}
```

### 7.3 Complete Deletion Before Approval
**Method**: `POST`  
**URL**: `{{base_service_url}}/api/tenant-deletions/{{deletion_request_id}}/complete`

**Headers**:
```json
{
  "Authorization": "Bearer {{admin_access_token}}"
}
```

**Expected Response**: `400 Bad Request`
```json
{
  "error": "Bad Request",
  "message": "Cannot complete deletion request that is not approved"
}
```

---

## Test Execution Order

### Phase 1: Setup
1. Generate User Token
2. Generate Admin Token
3. Create New Tenant
4. Create Building
5. Create Unit

### Phase 2: Deletion Workflow
6. Create Tenant Deletion Request
7. Get My Tenant Deletion Requests
8. Get All Tenant Deletion Requests (Admin)
9. Get Deletion Request Details (Admin)
10. Get Targets Status (Admin)
11. Approve Tenant Deletion Request (Admin)

### Phase 3: Building Deletion
12. Get Deleting Buildings (Admin)
13. Get My Deleting Buildings
14. Get Building Targets Status (Admin)
15. Complete Building Deletion (Admin)

### Phase 4: Complete Tenant Deletion
16. Complete Tenant Deletion (Admin)

### Phase 5: Verification
17. Verify Tenant Status
18. Verify Buildings Status
19. Verify Units Status

### Phase 6: Error Testing
20. Test unauthorized access
21. Test non-existent requests
22. Test invalid workflow states

---

## Environment Variables Setup Script

```javascript
// Pre-request script để setup environment variables
pm.environment.set('tenant_id', '550e8400-e29b-41d4-a716-446655440012');
pm.environment.set('user_id', '550e8400-e29b-41d4-a716-446655440011');
pm.environment.set('building_id', '550e8400-e29b-41d4-a716-446655440014');
pm.environment.set('unit_id', '550e8400-e29b-41d4-a716-446655440015');
pm.environment.set('deletion_request_id', '550e8400-e29b-41d4-a716-446655440016');
pm.environment.set('building_deletion_request_id', '550e8400-e29b-41d4-a716-446655440017');

console.log('=== QhomeBase 2025 Environment Variables ===');
console.log('- tenant_id:', pm.environment.get('tenant_id'));
console.log('- user_id:', pm.environment.get('user_id'));
console.log('- building_id:', pm.environment.get('building_id'));
console.log('- unit_id:', pm.environment.get('unit_id'));
console.log('- deletion_request_id:', pm.environment.get('deletion_request_id'));
console.log('- building_deletion_request_id:', pm.environment.get('building_deletion_request_id'));
console.log('===========================================');
```

---

## Common Test Assertions

```javascript
// Common test script cho tất cả requests
pm.test('Response time is less than 5000ms', function () {
    pm.expect(pm.response.responseTime).to.be.below(5000);
});

pm.test('Response has proper content type', function () {
    if (pm.response.headers.get('Content-Type')) {
        pm.expect(pm.response.headers.get('Content-Type')).to.include('application/json');
    }
});

// Log response for debugging
console.log('=== QhomeBase 2025 API Response ===');
console.log('Response Status:', pm.response.status);
console.log('Response Time:', pm.response.responseTime + 'ms');

if (pm.response.code >= 400) {
    console.log('Error Response:', pm.response.text());
} else {
    console.log('Success Response:', pm.response.text());
}
console.log('====================================');
```

---

## Notes

1. **Token Management**: Tokens sẽ được tự động extract và lưu vào environment variables
2. **ID Management**: Tất cả IDs sẽ được tự động extract từ responses và lưu vào environment variables
3. **Status Tracking**: Theo dõi status của tenant, buildings, và units qua các bước
4. **Error Handling**: Test các trường hợp lỗi để đảm bảo API hoạt động đúng
5. **Cleanup**: Sau khi test xong, có thể reset environment variables để test lại

## Troubleshooting

- **401 Unauthorized**: Kiểm tra token có hợp lệ không
- **404 Not Found**: Kiểm tra ID có đúng không
- **400 Bad Request**: Kiểm tra workflow state có đúng không
- **500 Internal Server Error**: Kiểm tra database connection và service status
