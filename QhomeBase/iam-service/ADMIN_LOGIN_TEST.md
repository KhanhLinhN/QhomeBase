# Admin Login Test Guide

## 🎯 Changes Summary

### 1. AuthService.java
- ✅ Support admin login with `tenantId = null` (global access)
- ✅ Admin can login to any tenant directly
- ✅ Admin gets ALL permissions
- ✅ Merge global + tenant roles for admin

### 2. AuthzService.java (base-service)
- ✅ `sameTenant()` now returns true for global admin
- ✅ Fixed `canRequestDeleteTenant()` to include sameTenant check
- ✅ Admin can delete any tenant

---

## 📊 Test Data

### 1. Create Global Admin User

```sql
-- Insert admin user
INSERT INTO iam.users (id, username, email, password_hash, active)
VALUES 
('11111111-1111-1111-1111-111111111111', 'superadmin', 'admin@qhome.com', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIrsf83/HZrnyMvAdEZJe4QrxU5f2PYO', true);
-- Password: admin123

-- Grant GLOBAL admin role (iam.user_roles, NOT user_tenant_roles!)
INSERT INTO iam.user_roles (user_id, role, granted_at, granted_by)
VALUES 
('11111111-1111-1111-1111-111111111111', 'admin', NOW(), 'system');

-- NO NEED to add to user_tenant_roles!
```

### 2. Verify Admin Role

```sql
-- Check global roles
SELECT * FROM iam.user_roles WHERE user_id = '11111111-1111-1111-1111-111111111111';

-- Result should show:
-- user_id = 11111111-1111-1111-1111-111111111111
-- role = admin
```

---

## 🧪 Test Scenarios

### ✅ Scenario 1: Admin Login Global (tenantId = null)

**Request:**
```json
POST http://localhost:8082/api/auth/login
Content-Type: application/json

{
  "username": "superadmin",
  "password": "admin123",
  "tenantId": null
}
```

**Expected Response: 200 OK**
```json
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "expiresAt": "2025-01-15T11:00:00Z",
  "userInfo": {
    "userId": "11111111-1111-1111-1111-111111111111",
    "username": "superadmin",
    "email": "admin@qhome.com",
    "tenantId": null,
    "tenantName": null,
    "roles": ["admin"],
    "permissions": [
      "base.tenant.create",
      "base.tenant.delete",
      "iam.user.create",
      "... all permissions ..."
    ]
  }
}
```

**Verify Token:**
```bash
# Decode JWT at https://jwt.io
# Payload should contain:
{
  "userId": "11111111-1111-1111-1111-111111111111",
  "username": "superadmin",
  "tenantId": null,
  "roles": ["admin"],
  "permissions": ["...all..."]
}
```

---

### ✅ Scenario 2: Admin Login to Specific Tenant

**Request:**
```json
POST http://localhost:8082/api/auth/login

{
  "username": "superadmin",
  "password": "admin123",
  "tenantId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Expected Response: 200 OK**
```json
{
  "userInfo": {
    "tenantId": "550e8400-e29b-41d4-a716-446655440001",
    "roles": ["admin"],
    "permissions": ["...all..."]
  }
}
```

---

### ✅ Scenario 3: Admin Can Access Any Tenant

**Test: Get tenant info**
```json
GET http://localhost:8080/api/base/tenants/550e8400-e29b-41d4-a716-446655440001
Authorization: Bearer <admin_global_token>

Expected: 200 OK (even if admin not assigned to this tenant)
```

**Test: Delete any tenant**
```json
POST http://localhost:8080/api/base/tenant-deletions
Authorization: Bearer <admin_global_token>

{
  "tenantId": "550e8400-e29b-41d4-a716-446655440002",
  "reason": "Admin cleaning up test tenant"
}

Expected: 200 OK (admin can delete ANY tenant)
```

---

### ✅ Scenario 4: Regular User (No Change)

**Request:**
```json
POST http://localhost:8082/api/auth/login

{
  "username": "owner_vcp",
  "password": "pass123",
  "tenantId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Expected: 200 OK (works as before)**

**Test: Regular user CANNOT delete other tenant**
```json
POST http://localhost:8080/api/base/tenant-deletions
Authorization: Bearer <owner_token>

{
  "tenantId": "550e8400-e29b-41d4-a716-446655440002",
  "reason": "Try to delete other tenant"
}

Expected: 403 Forbidden (sameTenant check fails)
```

---

### ❌ Scenario 5: Admin Without tenantId (OLD - Should Fail)

Before this fix:
```json
POST http://localhost:8082/api/auth/login

{
  "username": "superadmin",
  "password": "admin123",
  "tenantId": null
}

OLD Response: 400 Bad Request
{
  "error": "User has no access to any tenant"
}
```

After fix: ✅ 200 OK!

---

## 🔍 Verification Checklist

### After Login with tenantId = null:

- [ ] Token contains `"tenantId": null`
- [ ] Token contains `"roles": ["admin"]`
- [ ] Token contains ALL permissions (100+ permissions)
- [ ] Can GET /api/base/tenants (list all tenants)
- [ ] Can POST /api/base/tenant-deletions for ANY tenant
- [ ] Can POST /api/base/buildings for ANY tenant
- [ ] Can GET /api/iam/users (all users)

### After Login with specific tenantId:

- [ ] Token contains the specific `tenantId`
- [ ] Token contains `"roles": ["admin"]`
- [ ] Can access that tenant's resources
- [ ] Still has global admin powers

---

## 🐛 Common Issues

### Issue 1: Still getting "User has no access to any tenant"

**Cause:** Admin role is in `user_tenant_roles` instead of `user_roles`

**Fix:**
```sql
-- Delete wrong entry
DELETE FROM iam.user_tenant_roles WHERE user_id = 'admin-uuid' AND role = 'admin';

-- Add to correct table
INSERT INTO iam.user_roles (user_id, role) VALUES ('admin-uuid', 'admin');
```

---

### Issue 2: 403 Forbidden when accessing resources

**Cause:** Old token cached in frontend

**Fix:**
1. Logout
2. Clear localStorage
3. Login again with new token

---

### Issue 3: Admin can't delete tenant

**Cause:** `sameTenant()` not updated in AuthzService

**Fix:** Already fixed in this commit ✅

---

## 📋 Quick Test Commands

```bash
# 1. Create admin user
psql -U postgres -d qhome -c "INSERT INTO iam.users ..."

# 2. Grant admin role
psql -U postgres -d qhome -c "INSERT INTO iam.user_roles ..."

# 3. Test login
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123","tenantId":null}'

# 4. Test access
TOKEN="<your_token>"
curl http://localhost:8080/api/base/tenants \
  -H "Authorization: Bearer $TOKEN"
```

---

## ✅ Success Criteria

- [x] Admin can login with `tenantId = null`
- [x] Admin can login with specific `tenantId`
- [x] Admin has all permissions
- [x] Admin can access any tenant
- [x] Admin can delete any tenant
- [x] Regular users still work as before
- [x] Fixed authorization bug in `canRequestDeleteTenant`

