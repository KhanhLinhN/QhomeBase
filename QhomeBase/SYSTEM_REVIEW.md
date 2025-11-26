# Đánh giá hệ thống QhomeBase - Các chức năng còn thiếu

## 📋 Tổng quan
Hệ thống QhomeBase là một hệ thống quản lý căn hộ với các microservices:
- **base-service**: Quản lý building, unit, resident, household, vehicle, meter
- **finance-billing-service**: Quản lý hóa đơn, thanh toán, billing cycle
- **services-card-service**: Quản lý đăng ký thẻ (xe, cư dân, thang máy)
- **asset-maintenance-service**: Quản lý tài sản và bảo trì
- **customer-interaction-service**: Quản lý tin tức, thông báo, yêu cầu
- **iam-service**: Quản lý authentication, authorization
- **data-docs-service**: Quản lý hợp đồng và tài liệu

---

## ❌ Các chức năng còn thiếu

### 1. **Maintenance Request Management (Frontend)**
**Backend:** ✅ Có đầy đủ API trong `base-service`
- `POST /api/maintenance-requests` - Tạo yêu cầu
- `GET /api/maintenance-requests/my` - Lấy yêu cầu của cư dân
- `GET /api/maintenance-requests/admin` - Lấy tất cả yêu cầu (admin)
- `PUT /api/maintenance-requests/{id}/admin-response` - Admin phản hồi
- `PUT /api/maintenance-requests/{id}/approve` - Duyệt yêu cầu
- `PUT /api/maintenance-requests/{id}/reject` - Từ chối yêu cầu

**Frontend:** ❌ Chưa có
- Cần tạo trang quản lý yêu cầu sửa chữa cho admin
- Cần tạo trang tạo/xem yêu cầu sửa chữa cho cư dân (có thể trong Flutter app)

**Độ ưu tiên:** 🔴 Cao

---

### 2. **Cleaning Request Management (Frontend)**
**Backend:** ✅ Có đầy đủ API trong `base-service`
- `POST /api/cleaning-requests` - Tạo yêu cầu
- `GET /api/cleaning-requests/my` - Lấy yêu cầu của cư dân
- `GET /api/cleaning-requests/admin` - Lấy tất cả yêu cầu (admin)
- `PUT /api/cleaning-requests/{id}/approve` - Duyệt yêu cầu
- `PUT /api/cleaning-requests/{id}/reject` - Từ chối yêu cầu

**Frontend:** ❌ Chưa có
- Cần tạo trang quản lý yêu cầu dọn dẹp cho admin
- Cần tạo trang tạo/xem yêu cầu dọn dẹp cho cư dân

**Độ ưu tiên:** 🟡 Trung bình

---

### 3. **Pricing Tier Management (Frontend)**
**Backend:** ✅ Có đầy đủ API trong `finance-billing-service`
- `POST /api/pricing-tiers` - Tạo bậc giá
- `GET /api/pricing-tiers/service/{serviceCode}` - Lấy bậc giá theo dịch vụ
- `PUT /api/pricing-tiers/{id}` - Cập nhật bậc giá
- `DELETE /api/pricing-tiers/{id}` - Xóa bậc giá

**Frontend:** ✅ Đã có đầy đủ
- ✅ Có service `pricingTierService.ts` đầy đủ
- ✅ Có component `PricingFormulaModal.tsx` để quản lý bậc giá (CRUD đầy đủ)
- ✅ **Đã có trang quản lý riêng** `/base/finance/pricing-tiers`
- ✅ Đã có link trong sidebar (section "waterElectric")
- ✅ Hiển thị danh sách bậc giá, cho phép thêm/sửa/xóa
- ✅ Quản lý min/max quantity và unit price cho từng bậc
- ✅ Hỗ trợ cả ELECTRIC và WATER
- ✅ Quản lý effective date và active status

**Độ ưu tiên:** ✅ Đã hoàn thành

---

### 4. **Refund Management (Hoàn tiền)**
**Backend:** ⚠️ Có database schema nhưng chưa có API đầy đủ
- Có bảng `finance.refunds` trong migration V4
- Có enum `refund_status` (PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED, CANCELLED)
- Có comment trong code về refund nhưng chưa có implementation

**Cần:**
- Tạo API để tạo refund request
- Tạo API để duyệt/từ chối refund
- Tạo API để xử lý refund (tích hợp với payment gateway)
- Frontend để quản lý refund

**Độ ưu tiên:** 🟡 Trung bình

---

### 5. **Dashboard đầy đủ**
**Hiện tại:** ⚠️ Có dashboard cơ bản nhưng chưa đầy đủ

**Cần bổ sung:**
- **Admin Dashboard:**
  - Thống kê tổng quan: số building, unit, resident, vehicle
  - Thống kê tài chính: tổng doanh thu, hóa đơn chưa thanh toán, hóa đơn đã thanh toán
  - Thống kê yêu cầu: maintenance requests, cleaning requests, card registrations
  - Biểu đồ xu hướng theo tháng/quý/năm
  - Top 10 căn hộ có hóa đơn cao nhất
  - Thống kê theo tòa nhà

- **Resident Dashboard (Flutter app):**
  - Tổng quan hóa đơn: chưa thanh toán, đã thanh toán
  - Yêu cầu của tôi: maintenance, cleaning, card registration
  - Thông báo mới
  - Tin tức mới

**Độ ưu tiên:** 🟡 Trung bình

---

### 6. **Payment History & Reports**
**Hiện tại:** ⚠️ Có thể xem hóa đơn nhưng chưa có báo cáo chi tiết

**Cần:**
- Trang lịch sử thanh toán chi tiết
- Báo cáo thanh toán theo tháng/quý/năm
- Export báo cáo thanh toán ra Excel/PDF
- Thống kê phương thức thanh toán (VNPAY, tiền mặt, etc.)
- Báo cáo công nợ (căn hộ nào còn nợ)

**Độ ưu tiên:** 🟡 Trung bình

---

### 7. **Service Pricing Management (Frontend)**
**Backend:** ✅ Có API trong `finance-billing-service`
- `GET /api/service-pricing` - Lấy giá dịch vụ
- Có thể cần thêm API để quản lý giá dịch vụ

**Frontend:** ❌ Chưa có trang quản lý giá dịch vụ
- Quản lý giá cho các dịch vụ: Internet, Cable TV, Parking, etc.
- Có thể áp dụng giá theo thời gian (effective date)

**Độ ưen tiên:** 🟢 Thấp

---

### 8. **Notification Preferences**
**Hiện tại:** ✅ Có gửi notification nhưng chưa có quản lý preferences

**Cần:**
- Cho phép cư dân chọn loại thông báo muốn nhận
- Cài đặt thời gian nhận thông báo
- Tắt/bật thông báo theo loại (hóa đơn, yêu cầu, tin tức)

**Độ ưu tiên:** 🟢 Thấp

---

### 9. **Audit Log / Activity Log**
**Hiện tại:** ❌ Chưa có

**Cần:**
- Ghi log các thao tác quan trọng: tạo/sửa/xóa building, unit, invoice
- Hiển thị lịch sử thay đổi cho admin
- Audit trail cho các giao dịch tài chính

**Độ ưu tiên:** 🟡 Trung bình (quan trọng cho bảo mật và compliance)

---

### 10. **Multi-tenant Support (Frontend)**
**Backend:** ✅ Có hỗ trợ multi-tenant
**Frontend:** ⚠️ Có vẻ chưa đầy đủ
- Cần kiểm tra xem có switch tenant không
- Cần kiểm tra xem có quản lý tenant không

**Độ ưu tiên:** 🟡 Trung bình (nếu cần multi-tenant)

---

### 11. **Export/Import nâng cao**
**Hiện tại:** ✅ Đã có export cho:
- Buildings, Units, Meters, Invoices, Billing Cycles

**Cần bổ sung:**
- Export Residents
- Export Vehicles
- Export Maintenance Requests
- Export Cleaning Requests
- Import Residents (nếu cần)
- Import Vehicles (nếu cần)

**Độ ưu tiên:** 🟢 Thấp

---

### 12. **Search & Filter nâng cao**
**Hiện tại:** ⚠️ Có search/filter cơ bản

**Cần cải thiện:**
- Full-text search cho residents, invoices
- Advanced filter với nhiều điều kiện
- Saved filters
- Quick filters

**Độ ưu tiên:** 🟢 Thấp

---

### 13. **Profile Management cho Technician & Supporter**
**Backend:** ✅ Có API trong `iam-service`
- `GET /api/employees/{userId}` - Lấy thông tin employee
- `GET /api/employees/role/{roleName}` - Lấy employees theo role
- Có `UserProfileInfo` và `updateUserProfile` API

**Frontend:** ⚠️ Đã làm một phần
- ✅ Đã có trang `/staffProfile` cho technician/supporter
- ✅ Cho phép xem và cập nhật thông tin cá nhân (email, phone)
- ✅ Cho phép đổi mật khẩu
- ✅ Hiển thị thông tin cơ bản (username, email, phone, department, position, roles, status)
- ❌ **Thiếu:** Dashboard công việc:
  - Lịch sử assignments đã được giao
  - Lịch sử requests đã xử lý (maintenance, cleaning, etc.)
  - Thống kê công việc (số lượng yêu cầu đã xử lý, tỷ lệ hoàn thành, đánh giá nếu có)
- ❌ Chưa tích hợp vào Flutter app (có thể bỏ qua nếu không cần thiết)

**Cần bổ sung:**
- Thêm section dashboard công việc vào trang `/staffProfile`
- Hiển thị danh sách assignments/requests đã được giao và đã hoàn thành
- Thống kê số liệu công việc (tổng số, đã hoàn thành, đang xử lý, tỷ lệ hoàn thành)
- Có thể thêm biểu đồ thống kê theo thời gian

**Độ ưu tiên:** 🟡 Trung bình (đã có phần cơ bản, cần bổ sung dashboard)

---

## ✅ Các chức năng đã có đầy đủ

1. ✅ Building Management (CRUD + Import/Export)
2. ✅ Unit Management (CRUD + Import/Export)
3. ✅ Resident Management
4. ✅ Household Management
5. ✅ Vehicle Management
6. ✅ Card Registration (Vehicle, Resident, Elevator)
7. ✅ Card Pricing Management (vừa thêm)
8. ✅ Invoice Management (CRUD + Export)
9. ✅ Billing Cycle Management
10. ✅ Meter Reading & Assignment
11. ✅ Contract Management
12. ✅ News & Notification
13. ✅ Service Request (Customer Interaction)
14. ✅ Asset Management
15. ✅ Service Booking (Asset Maintenance)

---

## 🎯 Khuyến nghị ưu tiên

### Phase 1 (Cao - Cần làm ngay):
1. ✅ **Pricing Tier Management Frontend** - Đã hoàn thành
2. **Maintenance Request Management Frontend** - Chức năng cốt lõi cho cư dân
3. ✅ **Profile Management cho Technician & Supporter** - Đã hoàn thành

### Phase 2 (Trung bình - Làm sau):
4. **Cleaning Request Management Frontend**
5. **Dashboard đầy đủ**
6. **Payment History & Reports**
7. **Audit Log**

### Phase 3 (Thấp - Có thể làm sau):
8. **Refund Management**
9. **Service Pricing Management**
10. **Notification Preferences**
11. **Export/Import nâng cao**

---

## 📝 Ghi chú

- Hệ thống đã có backend khá đầy đủ
- Frontend còn thiếu một số trang quản lý
- Flutter app có vẻ đã có các chức năng cơ bản cho cư dân
- Cần kiểm tra kỹ hơn Flutter app để xem có thiếu gì không

