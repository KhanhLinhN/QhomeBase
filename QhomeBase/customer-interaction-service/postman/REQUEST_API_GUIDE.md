# 📋 Request Management API - Hướng dẫn sử dụng

## 📖 Tổng quan

API quản lý yêu cầu/khiếu nại khách hàng trong hệ thống QhomeBase. Cho phép:
- Tạo và quản lý yêu cầu/khiếu nại
- Theo dõi tiến trình xử lý
- Cập nhật trạng thái và log xử lý
- Thống kê và báo cáo

---

## 🚀 Cài đặt

### 1. Import Postman Collection
1. Mở Postman
2. Click **Import** > Chọn file:
   - `Request_Management_API.postman_collection.json`
   - `Request_Management_Environment.postman_environment.json`

### 2. Cấu hình Environment

Sau khi import, cập nhật các biến trong Environment:

| Variable | Mô tả | Ví dụ |
|----------|-------|-------|
| `baseUrl` | URL của service | `http://localhost:8086` |
| `tenantId` | ID của tenant | `123e4567-e89b-12d3-a456-426614174000` |
| `residentId` | ID của cư dân | `987fcdeb-51a2-43d7-8f9e-123456789abc` |
| `staffId` | ID của nhân viên | `456def78-90ab-12cd-34ef-567890abcdef` |
| `accessToken` | JWT token | `eyJhbGciOiJIUzI1Ni...` |

---

## 📝 Danh sách API

### 1️⃣ **Get Requests List (Filtered)**
```
GET /api/customer-interaction/requests
```

**Query Parameters:**
- `tenantId` (required): UUID của tenant
- `status`: PENDING, IN_PROGRESS, RESOLVED, CLOSED
- `priority`: LOW, MEDIUM, HIGH, URGENT
- `pageNo`: Số trang (default: 0)
- `projectCode`: Mã dự án/toà nhà
- `title`: Tiêu đề yêu cầu
- `residentName`: Tên cư dân
- `dateFrom`: Từ ngày (yyyy-MM-dd)
- `dateTo`: Đến ngày (yyyy-MM-dd)

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "requestCode": "REQ-2024-001",
      "tenantId": "uuid",
      "residentId": "uuid",
      "residentName": "Nguyễn Văn A",
      "title": "Khiếu nại về tiếng ồn",
      "content": "Mô tả chi tiết...",
      "status": "PENDING",
      "priority": "HIGH",
      "imagePath": "https://...",
      "createdAt": "2024-01-01 10:00:00",
      "updatedAt": "2024-01-02 15:30:00"
    }
  ],
  "pageable": {...},
  "totalElements": 50,
  "totalPages": 5
}
```

---

### 2️⃣ **Get Request Counts by Status**
```
GET /api/customer-interaction/requests/counts
```

**Response:**
```json
{
  "PENDING": 15,
  "IN_PROGRESS": 8,
  "RESOLVED": 42,
  "CLOSED": 135
}
```

---

### 3️⃣ **Get Request by ID**
```
GET /api/customer-interaction/requests/{id}
```

**Response:**
```json
{
  "id": "uuid",
  "requestCode": "REQ-2024-001",
  "tenantId": "uuid",
  "residentId": "uuid",
  "residentName": "Nguyễn Văn A",
  "title": "Khiếu nại về tiếng ồn",
  "content": "Mô tả chi tiết vấn đề...",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "imagePath": "https://example.com/image.jpg",
  "createdAt": "2024-01-01 10:00:00",
  "updatedAt": "2024-01-02 15:30:00"
}
```

---

### 4️⃣ **Create New Request (Complaint)**
```
POST /api/customer-interaction/requests/createRequest
```

**Request Body:**
```json
{
  "tenantId": "uuid",
  "residentId": "uuid",
  "residentName": "Nguyễn Văn A",
  "title": "Khiếu nại về tiếng ồn",
  "content": "Toà nhà phát ra tiếng ồn lớn vào ban đêm...",
  "priority": "HIGH",
  "status": "PENDING",
  "imagePath": "https://example.com/images/complaint.jpg"
}
```

**Priority Levels:**
- `LOW`: Thấp (không ảnh hưởng nhiều)
- `MEDIUM`: Trung bình (cần xử lý trong vài ngày)
- `HIGH`: Cao (cần xử lý nhanh)
- `URGENT`: Khẩn cấp (cần xử lý ngay)

---

### 5️⃣ **Get Processing Logs by Request ID**
```
GET /api/customer-interaction/requests-logs/{requestId}/logs
```

**Response:**
```json
[
  {
    "id": "uuid",
    "recordType": "REQUEST",
    "recordId": "uuid",
    "staffInCharge": "uuid",
    "staffInChargeName": "Trần Thị B - CSKH",
    "content": "Đã tiếp nhận yêu cầu",
    "requestStatus": "PENDING",
    "logType": "CREATED",
    "createdAt": "2024-01-01T10:00:00"
  },
  {
    "id": "uuid",
    "recordType": "REQUEST",
    "recordId": "uuid",
    "staffInCharge": "uuid",
    "staffInChargeName": "Trần Thị B - CSKH",
    "content": "Đã kiểm tra hiện trường",
    "requestStatus": "IN_PROGRESS",
    "logType": "STATUS_UPDATE",
    "createdAt": "2024-01-02T14:30:00"
  }
]
```

---

### 6️⃣ **Get Processing Logs by Staff ID**
```
GET /api/customer-interaction/requests-logs/staff/{staffId}
```

Lấy tất cả logs do một nhân viên xử lý.

---

### 7️⃣ **Add Processing Log (Update Request)**
```
POST /api/customer-interaction/requests-logs/{requestId}/logs
```

**Request Body:**
```json
{
  "recordType": "REQUEST",
  "staffInCharge": "uuid",
  "staffInChargeName": "Trần Thị B - Nhân viên CSKH",
  "content": "Đã kiểm tra và xác nhận vấn đề",
  "requestStatus": "IN_PROGRESS",
  "logType": "STATUS_UPDATE"
}
```

**Log Types:**
- `CREATED`: Tạo mới
- `STATUS_UPDATE`: Cập nhật trạng thái
- `COMMENT`: Bình luận
- `ATTACHMENT`: Đính kèm file
- `ASSIGNMENT`: Phân công
- `RESOLUTION`: Giải quyết
- `CLOSURE`: Đóng yêu cầu

---

### 8️⃣ **Resolve Request**
```
POST /api/customer-interaction/requests-logs/{requestId}/logs
```

**Request Body:**
```json
{
  "recordType": "REQUEST",
  "staffInCharge": "uuid",
  "staffInChargeName": "Trần Thị B - CSKH",
  "content": "Đã khắc phục xong vấn đề. Cư dân hài lòng.",
  "requestStatus": "RESOLVED",
  "logType": "RESOLUTION"
}
```

---

### 9️⃣ **Close Request**
```
POST /api/customer-interaction/requests-logs/{requestId}/logs
```

**Request Body:**
```json
{
  "recordType": "REQUEST",
  "staffInCharge": "uuid",
  "staffInChargeName": "Trần Thị B - CSKH",
  "content": "Yêu cầu đã được giải quyết hoàn tất. Đóng yêu cầu.",
  "requestStatus": "CLOSED",
  "logType": "CLOSURE"
}
```

---

## 🔄 Luồng xử lý chuẩn

```
1. PENDING (Chờ xử lý)
   ↓
2. IN_PROGRESS (Đang xử lý)
   ↓
3. RESOLVED (Đã giải quyết)
   ↓
4. CLOSED (Đã đóng)
```

### Chi tiết từng bước:

#### **Bước 1: Tạo yêu cầu**
- Cư dân tạo yêu cầu/khiếu nại
- Status: `PENDING`
- System tự động tạo `requestCode`

#### **Bước 2: Tiếp nhận & xử lý**
- Nhân viên tiếp nhận
- Cập nhật status → `IN_PROGRESS`
- Thêm log với nội dung xử lý

#### **Bước 3: Giải quyết**
- Hoàn tất xử lý vấn đề
- Cập nhật status → `RESOLVED`
- Log kết quả xử lý

#### **Bước 4: Đóng yêu cầu**
- Cư dân xác nhận hài lòng
- Cập nhật status → `CLOSED`
- Log hoàn tất

---

## 📊 Request Status

| Status | Mô tả | Màu hiển thị |
|--------|-------|--------------|
| `PENDING` | Chờ xử lý | 🟡 Vàng |
| `IN_PROGRESS` | Đang xử lý | 🔵 Xanh dương |
| `RESOLVED` | Đã giải quyết | 🟢 Xanh lá |
| `CLOSED` | Đã đóng | ⚫ Xám |

---

## 🎯 Priority Levels

| Priority | Thời gian xử lý | Mô tả |
|----------|----------------|-------|
| `LOW` | 5-7 ngày | Không ảnh hưởng nhiều |
| `MEDIUM` | 2-3 ngày | Cần xử lý sớm |
| `HIGH` | Trong ngày | Ảnh hưởng đáng kể |
| `URGENT` | Ngay lập tức | Khẩn cấp, ưu tiên cao nhất |

---

## 🔐 Authorization

Tất cả APIs đều yêu cầu JWT token trong header:
```
Authorization: Bearer <your-jwt-token>
```

---

## 🧪 Test Scenarios

### Scenario 1: Xử lý khiếu nại đầy đủ
```
1. Create New Request (Priority: HIGH)
2. Get Request by ID → Verify PENDING
3. Add Processing Log → Update to IN_PROGRESS
4. Add Processing Log → Update to RESOLVED
5. Add Processing Log → Update to CLOSED
6. Get Processing Logs → Verify all logs
```

### Scenario 2: Thống kê yêu cầu
```
1. Get Request Counts → Xem tổng quan
2. Get Requests List (filter by HIGH priority)
3. Get Requests List (filter by PENDING status)
```

### Scenario 3: Theo dõi công việc nhân viên
```
1. Get Processing Logs by Staff ID
2. Analyze workload and performance
```

---

## 💡 Tips

1. **Auto-save Request ID**: Collection tự động lưu `requestId` sau khi tạo request mới
2. **Filter effectively**: Sử dụng nhiều filter để tìm requests chính xác
3. **Log chi tiết**: Ghi log rõ ràng để dễ tracking
4. **Priority đúng**: Đặt priority phù hợp để xử lý hiệu quả

---

## 🐛 Troubleshooting

### Lỗi 401 Unauthorized
- Kiểm tra JWT token còn hiệu lực
- Verify token có đúng permissions

### Lỗi 400 Bad Request
- Kiểm tra format UUID
- Verify required fields

### Không tìm thấy request
- Kiểm tra `tenantId` đúng
- Verify `requestId` tồn tại

---

## 📞 Support

Nếu gặp vấn đề, vui lòng liên hệ team phát triển.

---

**Version:** 1.0  
**Last Updated:** 2024-10-25  
**Service Port:** 8086




