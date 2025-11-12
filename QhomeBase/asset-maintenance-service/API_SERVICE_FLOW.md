## Service Amenity API Blueprint

This document outlines the REST endpoints required to support the end-to-end facility service flow (configuration, booking, KPI tracking) in `asset-maintenance-service`. Paths presume a base prefix such as `/api/services`.

---

### 1. Service Category Management
- `GET /categories` – trả về danh sách danh mục kèm hỗ trợ lọc `isActive`, từ khóa; dùng cho trang quản trị hoặc dropdown khi tạo dịch vụ.
- `GET /categories/{id}` – lấy chi tiết một danh mục (mô tả, icon, thứ tự sắp xếp).
- `POST /categories` – tạo danh mục mới; yêu cầu `code`, `name`.
- `PUT /categories/{id}` – chỉnh sửa thông tin danh mục.
- `PATCH /categories/{id}/status` – kích hoạt/vô hiệu hóa danh mục mà không xóa dữ liệu.
- `DELETE /categories/{id}` – (tuỳ chọn) xóa danh mục; cân nhắc chỉ cho phép nếu không còn dịch vụ con.
- **Model/Table**: `ServiceCategory` ↔ `asset.service_category` (UUID, code, name, sortOrder, isActive). DTO: `ServiceCategoryDto`.

### 2. Service Configuration
- `GET /` – liệt kê dịch vụ; filter theo `categoryId`, `bookingType`, `isActive`, tên; hỗ trợ phân trang.
- `GET /{serviceId}` – trả về chi tiết dịch vụ; có thể kèm combos/options/tickets tùy query param (ví dụ `include=combos,options`).
- `POST /` – tạo dịch vụ mới với các trường cơ bản (code, tên, pricingType, bookingType, capacity...).
- `PUT /{serviceId}` – cập nhật thông tin dịch vụ (mô tả, giá, rule...).
- `PATCH /{serviceId}/status` – bật/tắt dịch vụ (ẩn khỏi cư dân nhưng giữ cấu hình).
- `GET /{serviceId}/availability` – xem lịch mở cửa theo ngày trong tuần.
- `PUT /{serviceId}/availability` – ghi đè toàn bộ lịch availability (sử dụng khi import excel).
- `POST /{serviceId}/availability` – thêm một khoảng thời gian mới (ví dụ mở thêm ca tối).
- `DELETE /availability/{availabilityId}` – xóa slot availability cụ thể.
- **Model/Table**: `Service` ↔ `asset.service` (pricingType `ServicePricingType`, bookingType `ServiceBookingType`, pricePerHour/session, cap, rules). Availability dùng `ServiceAvailability` ↔ `asset.service_availability`.
- **Query params chính**:  
  `categoryId` (UUID), `bookingType` (`STANDARD|COMBO_BASED|OPTION_BASED|TICKET_BASED`), `pricingType`, `isActive`, `search` (theo code/name), `page`, `size`, `sort`.
- **Payload chuẩn** (`POST`/`PUT`):
  ```json
  {
    "code": "SPA_BASIC",
    "name": "Gói spa căn bản",
    "description": "...",
    "pricingType": "SESSION",
    "bookingType": "COMBO_BASED",
    "pricePerHour": null,
    "pricePerSession": 550000,
    "maxCapacity": 4,
    "minDurationHours": 1,
    "maxDurationHours": 4,
    "advanceBookingDays": 14,
    "location": "Tầng 3",
    "mapUrl": "https://...",
    "rules": "Đặt trước 2 giờ",
    "isActive": true
  }
  ```
- **Availability DTO**:
  ```json
  {
    "dayOfWeek": 5,
    "startTime": "09:00",
    "endTime": "21:00",
    "isAvailable": true
  }
  ```
- **Validation**: code duy nhất (case-insensitive); minDuration ≤ maxDuration; startTime < endTime; `advanceBookingDays` ≥ 0.
- **Lifecycle**: service inactive vẫn giữ availability/combos → UI cư dân ẩn; admin vẫn cấu hình được.
- **Auditing**: lưu `createdAt`, `updatedAt`; cần log user thao tác trong service layer (`@AuditedAction` nếu có).

### 3. Combo Management
- `GET /{serviceId}/combos` – liệt kê combos của dịch vụ (tuỳ gửi kèm `isActive`).
- `GET /combos/{comboId}` – trả về combo và danh sách item đi kèm (dịch vụ con hoặc option).
- `POST /{serviceId}/combos` – tạo combo mới (code, tên, giá, thời lượng).
- `PUT /combos/{comboId}` – chỉnh sửa combo (mô tả, giá, thời lượng).
- `PATCH /combos/{comboId}/status` – bật/tắt combo, hữu ích khi promo kết thúc.
- `PUT /combos/{comboId}/items` – cấu hình chi tiết combo: danh sách dịch vụ phụ/option + quantity.
- `DELETE /combos/{comboId}` – xoá combo (có thể là soft delete để giữ lịch sử booking).
- **Model/Table**: `ServiceCombo` ↔ `asset.service_combo` (code, price, durationMinutes). Chi tiết trong `ServiceComboItem` ↔ `asset.service_combo_item` với quan hệ tới `Service`/`ServiceOption`.

### 4. Option & Option Group Management
- `GET /{serviceId}/options` – danh sách option add-on (than, nhân viên nướng,…).
- `GET /options/{optionId}` – chi tiết option, giá, trạng thái bắt buộc.
- `POST /{serviceId}/options` – tạo option mới.
- `PUT /options/{optionId}` – chỉnh sửa option.
- `PATCH /options/{optionId}/status` – bật/tắt option.
- `DELETE /options/{optionId}` – xóa option (hoặc chuyển sang inactive).
- `GET /{serviceId}/option-groups` – nhóm option (ví dụ “Chọn 1 loại bếp”, “Dịch vụ đi kèm”).
- `GET /option-groups/{groupId}` – chi tiết nhóm + item trong nhóm.
- `POST /{serviceId}/option-groups` – tạo nhóm option, định nghĩa `minSelect`, `maxSelect`.
- `PUT /option-groups/{groupId}` – chỉnh sửa tên/mô tả/giới hạn chọn.
- `PUT /option-groups/{groupId}/items` – gán option vào nhóm, sắp xếp thứ tự.
- `DELETE /option-groups/{groupId}` – xóa nhóm (nhớ cập nhật UI booking).
- **Model/Table**: `ServiceOption` ↔ `asset.service_option` (price, unit, isRequired). Nhóm dùng `ServiceOptionGroup` ↔ `asset.service_option_group` và bảng nối `ServiceOptionGroupItem`.
- **Validation**:
  - `code` duy nhất trong phạm vi service, không chứa khoảng trắng đầu/cuối.
  - `price` ≥ 0; `unit` bắt buộc nếu option tính theo đơn vị.
  - Khi isRequired=true thì option không được ẩn (`isActive=true`).
  - Nhóm: tất cả option trong group phải thuộc cùng `serviceId`; `minSelect` ≥ 0, `maxSelect` ≥ `minSelect`.
- **Payload mẫu** (`POST /{serviceId}/options`):
  ```json
  {
    "code": "EXTRA_CHARCOAL",
    "name": "Thêm 5kg than",
    "description": "Than sạch, cháy lâu",
    "price": 120000,
    "unit": "GÓI",
    "isRequired": false,
    "sortOrder": 10,
    "metadata": {
      "brand": "Than Đỏ",
      "weightKg": 5
    }
  }
  ```
- **Payload nhóm** (`POST /{serviceId}/option-groups` + `PUT /option-groups/{groupId}/items`):
  ```json
  {
    "code": "GRILL_TYPE",
    "name": "Chọn loại bếp",
    "description": "Bắt buộc chọn 1",
    "minSelect": 1,
    "maxSelect": 1,
    "sortOrder": 1
  }
  ```
  ```json
  {
    "items": [
      { "optionId": "UUID-OF-GAS", "sortOrder": 1 },
      { "optionId": "UUID-OF-CHARCOAL", "sortOrder": 2 }
    ]
  }
  ```
- **Business notes**:
  - Khi combo tham chiếu option, hệ thống lấy snapshot giá/metadata tại thời điểm cập nhật combo item để giữ lịch sử.
  - Booking validate theo group: cư dân phải chọn số lượng trong khoảng `[minSelect, maxSelect]`.
  - Option inactive sẽ bị loại khỏi nhóm/ combo và tự động ẩn khỏi các luồng booking.

### 5. Ticket Management
- `GET /{serviceId}/tickets` – danh sách vé sử dụng (day pass, family pass…).
- `GET /tickets/{ticketId}` – chi tiết vé (loại, giá, số người tối đa).
- `POST /{serviceId}/tickets` – tạo loại vé mới.
- `PUT /tickets/{ticketId}` – cập nhật vé.
- `PATCH /tickets/{ticketId}/status` – bật/tắt vé.
- `DELETE /tickets/{ticketId}` – xóa vé khỏi cấu hình.
- **Model/Table**: `ServiceTicket` ↔ `asset.service_ticket` (ticketType `ServiceTicketType`, durationHours, maxPeople).

### 6. Booking Flow (Resident/User side)
- `GET /{serviceId}/availability/slots` – trả về các khung giờ trống cho ngày/tuần; input gồm ngày bắt đầu/kết thúc, số người.
- `POST /bookings` – cư dân tạo booking, bao gồm combo/option/ticket đã chọn.
- `GET /bookings/{bookingId}` – xem chi tiết booking (thời gian, giá, trạng thái duyệt).
- `GET /bookings` – danh sách booking của người dùng (filter theo trạng thái, khoảng ngày).
- `PATCH /bookings/{bookingId}/cancel` – cư dân yêu cầu hủy, kiểm tra policy (deadline, phí phạt).
- `PATCH /bookings/{bookingId}/accept-terms` – xác nhận điều khoản sử dụng trước khi thanh toán.
- **Model/Table**: `ServiceBooking` ↔ `asset.service_booking` (status `ServiceBookingStatus`, paymentStatus `ServicePaymentStatus`, userId/approvedBy UUID). Slot data lấy từ `ServiceBookingSlot`.

### 7. Booking Administration
- `GET /admin/bookings` – màn hình quản trị: lọc theo dịch vụ, trạng thái, cư dân, ngày.
- `PATCH /admin/bookings/{bookingId}/approve` – phê duyệt (cập nhật `approvedBy`, `approvedAt`, giữ log).
- `PATCH /admin/bookings/{bookingId}/reject` – từ chối, ghi `rejectionReason`.
- `PATCH /admin/bookings/{bookingId}/complete` – đánh dấu hoàn thành sau khi dịch vụ diễn ra.
- `PATCH /admin/bookings/{bookingId}/slots` – chỉnh sửa slot (khi đổi ca thời gian).
- `PATCH /admin/bookings/{bookingId}/payment` – cập nhật trạng thái thanh toán (UNPAID → PAID), thông tin cổng.
- `DELETE /admin/bookings/{bookingId}` – soft delete nếu booking phát sinh lỗi; nên hạn chế.
- **Model/Table**: vẫn sử dụng `ServiceBooking` + quan hệ con `ServiceBookingSlot` và `ServiceBookingItem` cho line items.

### 8. Booking Items & Charges
- `GET /bookings/{bookingId}/items` – danh sách combo/option/ticket đã chọn.
- `POST /bookings/{bookingId}/items` – thêm item phát sinh (ví dụ thêm than khi cư dân yêu cầu).
- `PUT /bookings/{bookingId}/items/{itemId}` – cập nhật số lượng hoặc giá (áp dụng ưu đãi).
- `DELETE /bookings/{bookingId}/items/{itemId}` – xóa item, tự động cập nhật tổng tiền.
- **Model/Table**: `ServiceBookingItem` ↔ `asset.service_booking_item` (itemType `ServiceBookingItemType`, itemId tham chiếu UUID combinational, metadata JSONB).

### 9. KPI Management
- `GET /kpis/metrics` – liệt kê KPI đang theo dõi (filter theo dịch vụ, trạng thái).
- `GET /kpis/metrics/{metricId}` – xem định nghĩa KPI, bao gồm tần suất, phương pháp tính.
- `POST /kpis/metrics` – tạo KPI mới (ví dụ “Số lượt booking”, “Doanh thu”).
- `PUT /kpis/metrics/{metricId}` – chỉnh sửa mô tả, tần suất, cách tính.
- `PATCH /kpis/metrics/{metricId}/status` – dừng theo dõi KPI tạm thời.
- `POST /kpis/metrics/{metricId}/targets` – đặt mục tiêu cho kỳ (tháng/quý) cụ thể.
- `PUT /kpis/targets/{targetId}` – cập nhật giá trị mục tiêu hoặc người chịu trách nhiệm.
- `DELETE /kpis/targets/{targetId}` – xóa mục tiêu khi không còn áp dụng.
- `POST /kpis/metrics/{metricId}/values` – ghi nhận số liệu thực tế (manual hoặc system).
- `PUT /kpis/values/{valueId}` – điều chỉnh số liệu, cập nhật trạng thái DRAFT/FINAL.
- `GET /kpis/values` – xem lịch sử KPI (filter theo thời gian, dịch vụ, trạng thái).
- **Model/Table**: `ServiceKpiMetric` ↔ `asset.service_kpi_metric` (frequency `ServiceKpiFrequency`), `ServiceKpiTarget` ↔ `asset.service_kpi_target`, `ServiceKpiValue` ↔ `asset.service_kpi_value` (status/source enums).

### 10. Reporting & Dashboard
- `GET /reports/services/usage` – thống kê lượt booking/điểm sử dụng theo ngày/tuần/tháng.
- `GET /reports/services/revenue` – báo cáo doanh thu theo dịch vụ, loại sản phẩm (combo/option).
- `GET /reports/services/cancellation-rate` – tính tỷ lệ hủy, hỗ trợ KPI vận hành.
- `GET /reports/services/kpi-summary` – tổng hợp KPI: mục tiêu, giá trị thực tế, sai lệch.
- `GET /reports/services/{serviceId}/calendar` (tùy chọn) – trả về occupancy dạng calendar feed cho dashboard.
- **Nguồn dữ liệu**: tổng hợp từ `asset.service_booking`, `asset.service_booking_slot`, `asset.service_booking_item` và bảng KPI.

### 11. Supporting Endpoints / Integrations
- `GET /lookup/services/active` – danh sách dịch vụ đang hoạt động (id, name) cho UI/di động.
- `GET /lookup/services/{serviceId}/options` – các option đang mở bán, phục vụ màn booking.
- `GET /lookup/services/{serviceId}/combos` – danh sách combo đang active.
- `GET /lookup/services/{serviceId}/tickets` – các loại vé hiện có.
- Webhook / async: ví dụ callback từ payment gateway, hoặc publish event sang `customer-interaction-service` để gửi thông báo.
- **Model/Table**: dùng các bảng `asset.service`, `asset.service_option`, `asset.service_combo`, `asset.service_ticket` ở trạng thái active để feed cho UI / tích hợp.

---

> **Notes**
> - Apply consistent request/response DTOs (see `dto/service`) and pagination.
> - Secure endpoints: resident-facing vs admin; use IAM claims/scopes.
> - Consider idempotency for booking/payment endpoints.
> - Batch operations (e.g. availability import) can be added as needed.

