# Quản lý Trạng thái Hợp đồng (Contract Status Management)

## 1. Contract Status (Trạng thái Hợp đồng)

Trạng thái chính của hợp đồng, được lưu trong trường `status`:

### Các giá trị:

- **ACTIVE** (Mặc định)
  - Hợp đồng đang có hiệu lực
  - Được sử dụng cho các hợp đồng đang hoạt động
  - Chỉ hợp đồng ACTIVE mới có thể:
    - Gửi reminder gia hạn (renewal reminder)
    - Gia hạn (extend)
    - Checkout (dọn ra)

- **INACTIVE**
  - Hợp đồng chưa bắt đầu hiệu lực
  - Được tự động chuyển sang ACTIVE khi đến ngày bắt đầu (`startDate`)
  - Scheduled task chạy hàng ngày để activate các hợp đồng INACTIVE

- **CANCELLED**
  - Hợp đồng đã bị hủy
  - Được set khi:
    - **Cách 1:** Gọi `checkoutContract()` - set `checkoutDate` và tự động set `status = CANCELLED`
      - Khuyến nghị cho RENTAL contracts
    - **Cách 2:** Update contract và set `status = "CANCELLED"` thủ công
      - Qua endpoint `PUT /api/contracts/{contractId}` với body `{"status": "CANCELLED"}`
      - Áp dụng cho cả RENTAL và PURCHASE contracts
  - **⚠️ KHÔNG có endpoint riêng** để cancel contract (ví dụ: `PUT /api/contracts/{contractId}/cancel`)
  - Không thể sửa đổi hoặc activate lại
  - Không còn gửi renewal reminders

- **EXPIRED**
  - Hợp đồng đã hết hạn (endDate đã qua)
  - Tự động được đánh dấu khi `endDate < TODAY` (scheduled task chạy hàng ngày lúc 01:00)
  - **Lưu ý:** EXPIRED được set ngay khi `endDate < TODAY` (ngay cả 1 ngày sau endDate)
  - **KHÔNG phải** là quá 20 ngày. 20 ngày là thời gian để mark DECLINED (từ reminder đầu tiên)
  - Chỉ áp dụng cho hợp đồng có `endDate`
  - Không đánh dấu cho hợp đồng đã CANCELLED

- **TERMINATED**
  - Hợp đồng đã chấm dứt (theo comment trong migration)
  - Hiện tại chưa có logic tự động set status này
  - Có thể dùng để đánh dấu hợp đồng bị chấm dứt sớm (không phải do hết hạn)

### Logic chuyển đổi:

```
INACTIVE → ACTIVE (tự động khi đến startDate - scheduled 00:00)
ACTIVE → EXPIRED (tự động khi endDate < TODAY - scheduled 01:00)
ACTIVE → CANCELLED (manual: cancelContract hoặc checkoutContract)
ACTIVE → ACTIVE (extendContract - chỉ update endDate, giữ nguyên ACTIVE)
EXPIRED → ACTIVE (có thể manual nếu cần gia hạn lại - chưa có endpoint)
```

## 2. Contract Type (Loại Hợp đồng)

Loại hợp đồng, được lưu trong trường `contractType`:

- **RENTAL** (Mặc định)
  - Hợp đồng thuê
  - Có thể có `monthlyRent`
  - Có thể có `renewalStatus` (quản lý gia hạn)
  - Có thể checkout (set `checkoutDate`)

- **PURCHASE**
  - Hợp đồng mua
  - Có thể có `purchasePrice`
  - Có thể có `purchaseDate`
  - Không có `renewalStatus`

## 3. Renewal Status (Trạng thái Gia hạn)

**CHỈ ÁP DỤNG CHO RENTAL CONTRACTS**

Trạng thái gia hạn, được lưu trong trường `renewalStatus`:

### Các giá trị:

- **PENDING** (Mặc định)
  - Chưa gửi reminder gia hạn
  - Áp dụng cho hợp đồng mới tạo
  - Sẽ được chuyển sang REMINDED khi gửi reminder đầu tiên

- **REMINDED**
  - Đã gửi ít nhất 1 reminder gia hạn
  - `renewalReminderSentAt` chứa thời điểm gửi reminder đầu tiên
  - Có thể nhận reminder lần 2 (sau 7 ngày) và lần 3 (sau 20 ngày)
  - Sẽ chuyển sang DECLINED nếu quá 20 ngày không gia hạn

- **DECLINED**
  - Đã từ chối gia hạn (quá 20 ngày kể từ reminder đầu tiên)
  - `renewalDeclinedAt` chứa thời điểm đánh dấu declined
  - Không thể gửi reminder nữa

### Logic chuyển đổi:

```
PENDING → REMINDED (khi gửi reminder đầu tiên)
REMINDED → REMINDED (gửi reminder lần 2, 3 - không đổi status, chỉ update log)
REMINDED → DECLINED (sau 20+ ngày từ reminder đầu tiên)
DECLINED → PENDING (khi extendContract - reset để bắt đầu chu kỳ mới)
```

### Workflow Reminder:

1. **Reminder lần 1** (30 ngày trước khi hết hạn):
   - Tìm hợp đồng: `PENDING`, `ACTIVE`, `RENTAL`, `endDate` trong 30 ngày tới
   - Set `renewalReminderSentAt = NOW()`
   - Set `renewalStatus = REMINDED`

2. **Reminder lần 2** (7 ngày sau reminder lần 1):
   - Tìm hợp đồng: `REMINDED`, `ACTIVE`, `RENTAL`, `renewalReminderSentAt <= 7 ngày trước`
   - Không update `renewalReminderSentAt` (giữ nguyên thời điểm lần 1)
   - Giữ nguyên `renewalStatus = REMINDED`

3. **Reminder lần 3** (20 ngày sau reminder lần 1 - DEADLINE):
   - Tìm hợp đồng: `REMINDED`, `ACTIVE`, `RENTAL`, `renewalReminderSentAt <= 20 ngày trước`
   - Không update `renewalReminderSentAt`
   - Giữ nguyên `renewalStatus = REMINDED`

4. **Mark Declined** (Sau 20 ngày từ reminder lần 1):
   - Tìm hợp đồng: `REMINDED`, `ACTIVE`, `RENTAL`, `renewalReminderSentAt <= 20 ngày trước`
   - Set `renewalDeclinedAt = NOW()`
   - Set `renewalStatus = DECLINED`

5. **Extend Contract** (Gia hạn hợp đồng):
   - Update `endDate` thành ngày mới
   - Reset `renewalStatus = PENDING`
   - Reset `renewalReminderSentAt = NULL`
   - Reset `renewalDeclinedAt = NULL`

## 4. Checkout Date

Chỉ áp dụng cho RENTAL contracts:

- `checkoutDate`: Ngày dọn ra
- Được set khi gọi `checkoutContract()`
- Khi checkout, contract sẽ được set status = `CANCELLED` (hủy hợp đồng)

## 5. Các Trường Quan Trọng

```java
// Contract Status
status: String (ACTIVE, INACTIVE, CANCELLED, EXPIRED, TERMINATED)

// Contract Type  
contractType: String (RENTAL, PURCHASE)

// Renewal Status (chỉ cho RENTAL)
renewalStatus: String (PENDING, REMINDED, DECLINED)
renewalReminderSentAt: OffsetDateTime (thời điểm gửi reminder đầu tiên)
renewalDeclinedAt: OffsetDateTime (thời điểm đánh dấu declined)

// Dates
startDate: LocalDate (ngày bắt đầu)
endDate: LocalDate (ngày kết thúc)
checkoutDate: LocalDate (ngày dọn ra - chỉ cho RENTAL)
```

## 6. Scheduled Tasks

### 1. Activate Inactive Contracts (Chạy hàng ngày lúc 00:00)
- Tìm hợp đồng: `INACTIVE`, `startDate = TODAY`
- Chuyển sang `ACTIVE`

### 2. Mark Expired Contracts (Chạy hàng ngày lúc 01:00)
- Tìm hợp đồng: `ACTIVE`, `endDate IS NOT NULL`, `endDate < TODAY`
- Chuyển sang `EXPIRED`
- Không đánh dấu cho hợp đồng đã CANCELLED hoặc không có endDate

### 3. Send Renewal Reminders (Chạy hàng ngày lúc 08:00)
- Gửi reminder lần 1, 2, 3 cho các hợp đồng RENTAL đang ACTIVE
- Chỉ xử lý hợp đồng có `endDate` trong tương lai
- Không gửi reminder cho hợp đồng đã EXPIRED

### 4. Mark Renewal Declined (Chạy hàng ngày lúc 09:00)
- Đánh dấu DECLINED cho hợp đồng quá 20 ngày từ reminder đầu tiên

## 7. Manual Endpoints

### Contract Operations:
- `POST /api/contracts/{contractId}/checkout`: Checkout hợp đồng (set checkoutDate, status = CANCELLED)
- `PUT /api/contracts/{contractId}/extend`: Gia hạn hợp đồng (update endDate, reset renewal status)
- `PUT /api/contracts/{contractId}/cancel`: Hủy hợp đồng (set status = CANCELLED)

### Testing/Manual Triggers:
- `POST /api/contracts/renewal/trigger-reminders`: Trigger reminders thủ công (testing)
- `POST /api/contracts/renewal/trigger-declined`: Trigger mark declined thủ công (testing)
- `POST /api/contracts/status/trigger-expired`: Trigger mark expired thủ công (testing)

